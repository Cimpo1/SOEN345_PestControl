package com.pestcontrol.backend.integration.controller;

import com.pestcontrol.backend.api.EventController;
import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.LocationRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.service.EventService;
import com.pestcontrol.backend.service.JWTService;
import com.pestcontrol.backend.service.ReservationEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerIntegrationTest {

    @Mock EventRepository eventRepository;
    @Mock LocationRepository locationRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock TicketRepository ticketRepository;
    @Mock ReservationEmailService reservationEmailService;

    private EventController eventController;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        EventService eventService = new EventService(
                eventRepository,
                locationRepository,
                reservationRepository,
                ticketRepository,
                reservationEmailService);
        eventController = new EventController(eventService);

        adminToken    = "Bearer " + JWTService.generateToken(buildUser(1L, UserRole.ADMIN));
        customerToken = "Bearer " + JWTService.generateToken(buildUser(2L, UserRole.CUSTOMER));

        lenient().when(eventRepository.findByStatus(EventStatus.SCHEDULED)).thenReturn(Collections.emptyList());
    }

    @Test
    void getEvents_noFilters_returns200WithList() {
        when(eventRepository.findAll()).thenReturn(List.of(buildEvent(1L, EventStatus.SCHEDULED)));

        ResponseEntity<List<EventResponse>> response =
                eventController.getEvents(null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository).findAll();
    }

    @Test
    void getEvents_withInvalidCategory_throws400BeforeHittingRepo() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getEvents(null, null, null, null, "NOT_A_CATEGORY"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(eventRepository, never()).findAll();
    }

    @Test
    void getEvents_withEndDateBeforeStartDate_throws400() {
        LocalDate startDate = java.time.LocalDate.of(2099, 1, 1);
        LocalDate endDate = java.time.LocalDate.of(2099, 12, 31);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getEvents(
                        null,
                        endDate,
                        startDate,
                        null,
                        null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getEventById_existingId_returns200() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(buildEvent(1L, EventStatus.SCHEDULED)));

        ResponseEntity<EventResponse> response = eventController.getEventById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository).findById(1L);
    }

    @Test
    void getEventById_unknownId_throws404() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getEventById(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getAdminEvents_withAdminToken_returns200() {
        when(eventRepository.findAll())
                .thenReturn(List.of(buildEvent(1L, EventStatus.SCHEDULED)));

        ResponseEntity<List<EventResponse>> response =
                eventController.getAdminEvents(adminToken, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository).findAll();
    }

    @Test
    void getAdminEvents_withNoAuthHeader_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getAdminEvents(null, null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(eventRepository, never()).findAll();
    }

    @Test
    void getAdminEvents_withCustomerToken_throws403() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getAdminEvents(customerToken, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(eventRepository, never()).findAll();
    }

    @Test
    void getAdminEvents_withStatusFilter_queriesRepoByStatus() {
        when(eventRepository.findByStatus(EventStatus.CANCELLED))
                .thenReturn(List.of(buildEvent(2L, EventStatus.CANCELLED)));

        ResponseEntity<List<EventResponse>> response =
                eventController.getAdminEvents(adminToken, "CANCELLED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository).findByStatus(EventStatus.CANCELLED);
        verify(eventRepository, never()).findAll();
    }

    @Test
    void getAdminEvents_withInvalidStatus_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getAdminEvents(adminToken, "WRONG"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createEvent_withAdminTokenAndValidBody_returns201() {
        Location location = buildLocation(1L);
        Event saved = buildEvent(1L, EventStatus.SCHEDULED);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        ResponseEntity<EventResponse> response =
                eventController.createEvent(adminToken, buildCreateRequest(1L));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_withoutAuthHeader_throws401() {
        var request = buildCreateRequest(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.createEvent(null, request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_withCustomerToken_throws403() {
        var request = buildCreateRequest(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.createEvent(customerToken,request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_withMissingTitle_throws400() {
        CreateEventRequest request = buildCreateRequest(1L);
        request.setTitle(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.createEvent(adminToken, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_withEndDateBeforeStartDate_throws400() {
        CreateEventRequest request = buildCreateRequest(1L);
        request.setStartDateTime(OffsetDateTime.now().plusDays(3));
        request.setEndDateTime(OffsetDateTime.now().plusDays(1));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.createEvent(adminToken, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_withNeitherLocationIdNorInlineLocation_throws400() {
        CreateEventRequest request = buildCreateRequest(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.createEvent(adminToken, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEvent_withAdminTokenAndScheduledEvent_returns200() {
        Location location = buildLocation(1L);
        Event existing = buildEvent(1L, EventStatus.SCHEDULED);
        Event saved    = buildEvent(1L, EventStatus.SCHEDULED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);
        when(reservationRepository.findByEvent(any(Event.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<EventResponse> response =
                eventController.updateEvent(adminToken, 1L, buildUpdateRequest(1L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateEvent_onCancelledEvent_throws409() {
        Event cancelled = buildEvent(1L, EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(cancelled));
        var request = buildUpdateRequest(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.updateEvent(adminToken, 1L, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEvent_unknownId_throws404() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());
        var request = buildUpdateRequest(1L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.updateEvent(adminToken, 99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void cancelEvent_scheduledEvent_returns200() {
        Event event = buildEvent(1L, EventStatus.SCHEDULED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(reservationRepository.findByEvent(any(Event.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<EventResponse> response =
                eventController.cancelEvent(adminToken, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void cancelEvent_alreadyCancelled_throws409() {
        Event event = buildEvent(1L, EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.cancelEvent(adminToken, 1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void cancelEvent_pastEvent_throws400() {
        Event event = buildEvent(1L, EventStatus.PAST);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.cancelEvent(adminToken, 1L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void cancelEvent_withoutAuthHeader_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.cancelEvent(null, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(eventRepository, never()).save(any());
    }

    private User buildUser(Long id, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setFullName("Test User");
        user.setEmail("test" + id + "@example.com");
        user.setUserRole(role);
        user.setPasswordHash("irrelevant_hash");
        return user;
    }

    private Location buildLocation(Long id) {
        Location location = new Location("Test Venue", "123 Main St", "Montreal", "QC", "H1A 1A1");
        location.setLocationId(id);
        return location;
    }

    private Event buildEvent(Long id, EventStatus status) {
        Event event = new Event(
                buildLocation(1L),
                "Test Event",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(3),
                Category.SPORTS,
                BigDecimal.valueOf(25.00),
                status);
        event.setEventId(id);
        return event;
    }

    private CreateEventRequest buildCreateRequest(Long locationId) {
        CreateEventRequest r = new CreateEventRequest();
        r.setTitle("Test Event");
        r.setStartDateTime(OffsetDateTime.now().plusDays(2));
        r.setEndDateTime(OffsetDateTime.now().plusDays(2).plusHours(3));
        r.setCategory("SPORTS");
        r.setBasePrice(BigDecimal.valueOf(50.00));
        r.setLocationId(locationId);
        return r;
    }

    private UpdateEventRequest buildUpdateRequest(Long locationId) {
        UpdateEventRequest r = new UpdateEventRequest();
        r.setTitle("Updated Event");
        r.setStartDateTime(OffsetDateTime.now().plusDays(3));
        r.setEndDateTime(OffsetDateTime.now().plusDays(3).plusHours(3));
        r.setCategory("SPORTS");
        r.setBasePrice(BigDecimal.valueOf(75.00));
        r.setLocationId(locationId);
        return r;
    }
}