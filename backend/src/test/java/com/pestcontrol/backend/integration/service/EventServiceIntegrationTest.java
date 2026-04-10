package com.pestcontrol.backend.integration.service;

import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventLocationRequest;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.TicketStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.EventService;
import com.pestcontrol.backend.service.ReservationEmailService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventServiceIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventService eventService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private ReservationEmailService reservationEmailService;

    private Location defaultLocation;

    @BeforeEach
    void setUp() {
        defaultLocation = new Location("Bell Centre", "1909 Avenue des Canadiens-de-Montréal", "Montreal", "QC", "H3B 5E8");
        entityManager.persist(defaultLocation);
        entityManager.flush();
    }

    @Test
    void getEventById_whenExists_returnsEvent() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Solo Show", Category.CONCERT,
                OffsetDateTime.parse("2026-09-01T20:00:00Z")));

        EventResponse response = eventService.getEventById(event.getEventId());

        assertEquals("Solo Show", response.getTitle());
        assertEquals(Category.CONCERT, response.getCategory());
    }

    @Test
    void getEventById_whenNotFound_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getEventById(99999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getEvents_withNoFilters_returnsOnlyScheduledEvents() {
        eventRepository.save(buildEventWithStatus(defaultLocation, "Active Concert", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z"), EventStatus.SCHEDULED));
        eventRepository.save(buildEventWithStatus(defaultLocation, "Cancelled Gig", Category.CONCERT,
                OffsetDateTime.parse("2026-10-02T20:00:00Z"), EventStatus.CANCELLED));
        eventRepository.save(buildEventWithStatus(defaultLocation, "Past Show", Category.CONCERT,
                OffsetDateTime.parse("2026-10-03T20:00:00Z"), EventStatus.PAST));

        List<EventResponse> results = eventService.getEvents(null, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals("Active Concert", results.get(0).getTitle());
    }
    @Test
    void getEvents_withNoFilters_returnsSortedByStartDateTime() {
        eventRepository.save(buildEvent(defaultLocation, "Event C", Category.CONCERT,
                OffsetDateTime.parse("2026-12-01T20:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Event A", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Event B", Category.CONCERT,
                OffsetDateTime.parse("2026-11-01T20:00:00Z")));

        List<EventResponse> results = eventService.getEvents(null, null, null, null, null);

        assertEquals(List.of("Event A", "Event B", "Event C"),
                results.stream().map(EventResponse::getTitle).toList());
    }
    @Test
    void getEvents_withTitleFilter_returnsCaseInsensitiveMatches() {
        eventRepository.save(buildEvent(defaultLocation, "Summer Jazz Festival", Category.CONCERT,
                OffsetDateTime.parse("2026-07-15T18:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Winter Gala", Category.FAMILY,
                OffsetDateTime.parse("2026-12-20T18:00:00Z")));

        List<EventResponse> results = eventService.getEvents("JAZZ", null, null, null, null);

        assertEquals(1, results.size());
        assertEquals("Summer Jazz Festival", results.get(0).getTitle());
    }
    @Test
    void getEvents_withTitleFilter_whenNoMatch_returnsEmpty() {
        eventRepository.save(buildEvent(defaultLocation, "Comedy Night", Category.COMEDY,
                OffsetDateTime.parse("2026-07-10T19:00:00Z")));

        List<EventResponse> results = eventService.getEvents("ballet", null, null, null, null);

        assertTrue(results.isEmpty());
    }
    @Test
    void getEvents_withStartDate_excludesEarlierEvents() {
        eventRepository.save(buildEvent(defaultLocation, "Early Bird", Category.SPORTS,
                OffsetDateTime.parse("2026-05-01T10:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Late Show", Category.SPORTS,
                OffsetDateTime.parse("2026-09-01T10:00:00Z")));

        List<EventResponse> results = eventService.getEvents(null, LocalDate.of(2026, 7, 1), null, null, null);

        assertEquals(1, results.size());
        assertEquals("Late Show", results.get(0).getTitle());
    }

    @Test
    void getEvents_withEndDate_excludesLaterEvents() {
        eventRepository.save(buildEvent(defaultLocation, "Spring Fling", Category.SPORTS,
                OffsetDateTime.parse("2026-04-15T10:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Fall Fest", Category.SPORTS,
                OffsetDateTime.parse("2026-10-15T10:00:00Z")));

        LocalDate endDate = java.time.LocalDate.of(2026, 6, 30);

        List<EventResponse> results = eventService.getEvents(null, null, endDate, null, null);

        assertEquals(1, results.size());
        assertEquals("Spring Fling", results.get(0).getTitle());
    }
    @Test
    void getEvents_withEndDateBeforeStartDate_throwsBadRequest() {
        LocalDate startDate = java.time.LocalDate.of(2026, 9, 1);
        LocalDate endDate = java.time.LocalDate.of(2026, 8, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getEvents(null,
                        startDate,
                        endDate,
                        null, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
    @Test
    void getEvents_withLocationFilter_matchesVenueName() {
        Location otherLocation = new Location("Place Bell", "21 Boulevard Chomedey", "Laval", "QC", "H7X 3S6");
        entityManager.persist(otherLocation);
        entityManager.flush();

        eventRepository.save(buildEvent(defaultLocation, "Bell Centre Event", Category.SPORTS,
                OffsetDateTime.parse("2026-08-10T19:00:00Z")));
        eventRepository.save(buildEvent(otherLocation, "Laval Event", Category.SPORTS,
                OffsetDateTime.parse("2026-08-11T19:00:00Z")));

        List<EventResponse> results = eventService.getEvents(null, null, null, "Bell Centre", null);

        assertEquals(1, results.size());
        assertEquals("Bell Centre Event", results.get(0).getTitle());
    }

    @Test
    void getEvents_withLocationFilter_matchesCityName() {
        Location lavalLocation = new Location("Arena Laval", "123 Laval St", "Laval", "QC", "H7A 1A1");
        entityManager.persist(lavalLocation);
        entityManager.flush();

        eventRepository.save(buildEvent(defaultLocation, "MTL Event", Category.SPORTS,
                OffsetDateTime.parse("2026-08-10T19:00:00Z")));
        eventRepository.save(buildEvent(lavalLocation, "Laval Event", Category.SPORTS,
                OffsetDateTime.parse("2026-08-11T19:00:00Z")));

        List<EventResponse> results = eventService.getEvents(null, null, null, "laval", null);

        assertEquals(1, results.size());
        assertEquals("Laval Event", results.get(0).getTitle());
    }
    @Test
    void getEvents_withSingleCategory_returnsOnlyMatchingCategory() {
        eventRepository.save(buildEvent(defaultLocation, "Rock Night", Category.CONCERT,
                OffsetDateTime.parse("2026-09-05T20:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Family Fun Day", Category.FAMILY,
                OffsetDateTime.parse("2026-09-06T12:00:00Z")));

        List<EventResponse> results = eventService.getEvents(null, null, null, null, List.of(Category.FAMILY));

        assertEquals(1, results.size());
        assertEquals("Family Fun Day", results.get(0).getTitle());
    }

    @Test
    void getEvents_withMultipleCategories_returnsAllMatching() {
        eventRepository.save(buildEvent(defaultLocation, "Stand-Up Night", Category.COMEDY,
                OffsetDateTime.parse("2026-09-10T19:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Art Show", Category.ART_THEATER,
                OffsetDateTime.parse("2026-09-11T15:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Sports Game", Category.SPORTS,
                OffsetDateTime.parse("2026-09-12T18:00:00Z")));

        List<EventResponse> results = eventService.getEvents(null, null, null, null,
                List.of(Category.COMEDY, Category.ART_THEATER));

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(e -> e.getTitle().equals("Stand-Up Night")));
        assertTrue(results.stream().anyMatch(e -> e.getTitle().equals("Art Show")));
    }
    @Test
    void getAdminEvents_withNoFilter_returnsAllStatuses() {
        eventRepository.save(buildEventWithStatus(defaultLocation, "Scheduled Show", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z"), EventStatus.SCHEDULED));
        eventRepository.save(buildEventWithStatus(defaultLocation, "Cancelled Gig", Category.CONCERT,
                OffsetDateTime.parse("2026-10-02T20:00:00Z"), EventStatus.CANCELLED));
        eventRepository.save(buildEventWithStatus(defaultLocation, "Past Concert", Category.CONCERT,
                OffsetDateTime.parse("2026-10-03T20:00:00Z"), EventStatus.PAST));

        List<EventResponse> results = eventService.getAdminEvents(null);

        assertEquals(3, results.size());
    }

    @Test
    void getAdminEvents_withBlankStatus_returnsAllStatuses() {
        eventRepository.save(buildEventWithStatus(defaultLocation, "Show A", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z"), EventStatus.SCHEDULED));
        eventRepository.save(buildEventWithStatus(defaultLocation, "Show B", Category.CONCERT,
                OffsetDateTime.parse("2026-10-02T20:00:00Z"), EventStatus.CANCELLED));

        List<EventResponse> results = eventService.getAdminEvents("   ");

        assertEquals(2, results.size());
    }

    @Test
    void getAdminEvents_withStatusFilter_returnsOnlyMatchingStatus() {
        eventRepository.save(buildEventWithStatus(defaultLocation, "Scheduled Show", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z"), EventStatus.SCHEDULED));
        eventRepository.save(buildEventWithStatus(defaultLocation, "Cancelled Gig", Category.CONCERT,
                OffsetDateTime.parse("2026-10-02T20:00:00Z"), EventStatus.CANCELLED));

        List<EventResponse> results = eventService.getAdminEvents("CANCELLED");

        assertEquals(1, results.size());
        assertEquals("Cancelled Gig", results.get(0).getTitle());
    }

    @Test
    void getAdminEvents_withInvalidStatus_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getAdminEvents("NOT_A_STATUS"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getAdminEvents_returnsSortedByStartDateTimeDescending() {
        eventRepository.save(buildEvent(defaultLocation, "Event A", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Event B", Category.CONCERT,
                OffsetDateTime.parse("2026-12-01T20:00:00Z")));
        eventRepository.save(buildEvent(defaultLocation, "Event C", Category.CONCERT,
                OffsetDateTime.parse("2026-11-01T20:00:00Z")));

        List<EventResponse> results = eventService.getAdminEvents(null);

        assertEquals(List.of("Event B", "Event C", "Event A"),
                results.stream().map(EventResponse::getTitle).toList());
    }
    @Test
    void getEvents_autoTransitionsPastScheduledEvents() {
        eventRepository.save(buildEventWithStatus(defaultLocation, "Already Ended", Category.CONCERT,
                OffsetDateTime.parse("2024-01-01T10:00:00Z"), EventStatus.SCHEDULED));

        List<EventResponse> results = eventService.getEvents(null, null, null, null, null);

        assertTrue(results.isEmpty());
    }

    @Test
    void getAdminEvents_autoTransitionsPastScheduledEvents() {
        eventRepository.save(buildEventWithStatus(defaultLocation, "Already Ended", Category.CONCERT,
                OffsetDateTime.parse("2024-01-01T10:00:00Z"), EventStatus.SCHEDULED));

        List<EventResponse> adminResults = eventService.getAdminEvents("PAST");

        assertEquals(1, adminResults.size());
        assertEquals("Already Ended", adminResults.get(0).getTitle());
    }
    @Test
    void cancelEvent_whenScheduled_setsStatusToCancelled() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Cancellable Show", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z")));

        EventResponse response = eventService.cancelEvent(null, event.getEventId());

        assertEquals(EventStatus.CANCELLED, response.getStatus());
    }

    @Test
    void cancelEvent_whenAlreadyCancelled_throwsConflict() {
        Event event = eventRepository.save(buildEventWithStatus(defaultLocation, "Already Cancelled", Category.CONCERT,
                OffsetDateTime.parse("2026-10-01T20:00:00Z"), EventStatus.CANCELLED));

        Long eventId = event.getEventId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.cancelEvent(null, eventId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void cancelEvent_whenPast_throwsBadRequest() {
        Event event = eventRepository.save(buildEventWithStatus(defaultLocation, "Past Show", Category.CONCERT,
                OffsetDateTime.parse("2024-01-01T10:00:00Z"), EventStatus.PAST));

        Long eventId = event.getEventId();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.cancelEvent(null, eventId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void cancelEvent_whenNotFound_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.cancelEvent(null, 99999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
    @Test
    void createEvent_withValidLocationId_savesAndReturnsEvent() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("New Show");
        request.setStartDateTime(OffsetDateTime.now().plusDays(10));
        request.setEndDateTime(OffsetDateTime.now().plusDays(10).plusHours(2));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("29.99"));
        request.setLocationId(defaultLocation.getLocationId());

        EventResponse response = eventService.createEvent(null, request);

        assertEquals("New Show", response.getTitle());
        assertEquals(Category.CONCERT, response.getCategory());
        assertNotNull(response.getEventId());
    }

    @Test
    void createEvent_withInlineLocation_persistsNewLocation() {
        EventLocationRequest locationRequest = new EventLocationRequest();
        locationRequest.setName("New Venue");
        locationRequest.setAddressLine("456 New St");
        locationRequest.setCity("Quebec City");
        locationRequest.setProvince("QC");
        locationRequest.setPostalCode("G1A 1A1");

        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Venue Test");
        request.setStartDateTime(OffsetDateTime.now().plusDays(5));
        request.setEndDateTime(OffsetDateTime.now().plusDays(5).plusHours(3));
        request.setCategory("SPORTS");
        request.setBasePrice(new BigDecimal("15.00"));
        request.setLocation(locationRequest);

        EventResponse response = eventService.createEvent(null, request);

        assertEquals("New Venue", response.getLocation().getName());
    }

    @Test
    void createEvent_withNoLocationIdOrPayload_throwsBadRequest() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("No Location");
        request.setStartDateTime(OffsetDateTime.now().plusDays(5));
        request.setEndDateTime(OffsetDateTime.now().plusDays(5).plusHours(2));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("10.00"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(null, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createEvent_withInvalidLocationId_throwsNotFound() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Ghost Venue");
        request.setStartDateTime(OffsetDateTime.now().plusDays(5));
        request.setEndDateTime(OffsetDateTime.now().plusDays(5).plusHours(2));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("10.00"));
        request.setLocationId(99999L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(null, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createEvent_withStartDateInPast_throwsBadRequest() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Past Event");
        request.setStartDateTime(OffsetDateTime.now().minusDays(1));
        request.setEndDateTime(OffsetDateTime.now().plusHours(1));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("10.00"));
        request.setLocationId(defaultLocation.getLocationId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(null, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createEvent_withEndDateBeforeStartDate_throwsBadRequest() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Bad Dates");
        request.setStartDateTime(OffsetDateTime.now().plusDays(5));
        request.setEndDateTime(OffsetDateTime.now().plusDays(3));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("10.00"));
        request.setLocationId(defaultLocation.getLocationId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(null, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createEvent_withZeroBasePrice_throwsBadRequest() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Free Show");
        request.setStartDateTime(OffsetDateTime.now().plusDays(5));
        request.setEndDateTime(OffsetDateTime.now().plusDays(5).plusHours(2));
        request.setCategory("CONCERT");
        request.setBasePrice(BigDecimal.ZERO);
        request.setLocationId(defaultLocation.getLocationId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(null, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createEvent_withInvalidCategory_throwsBadRequest() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Bad Category");
        request.setStartDateTime(OffsetDateTime.now().plusDays(5));
        request.setEndDateTime(OffsetDateTime.now().plusDays(5).plusHours(2));
        request.setCategory("INVALID");
        request.setBasePrice(new BigDecimal("10.00"));
        request.setLocationId(defaultLocation.getLocationId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.createEvent(null, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateEvent_withValidRequest_updatesAndReturnsEvent() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Original Title", Category.CONCERT,
                OffsetDateTime.now().plusDays(10)));

        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Updated Title");
        request.setStartDateTime(OffsetDateTime.now().plusDays(10));
        request.setEndDateTime(OffsetDateTime.now().plusDays(10).plusHours(3));
        request.setCategory("SPORTS");
        request.setBasePrice(new BigDecimal("75.00"));
        request.setLocationId(defaultLocation.getLocationId());

        EventResponse response = eventService.updateEvent(null, event.getEventId(), request);

        assertEquals("Updated Title", response.getTitle());
        assertEquals(Category.SPORTS, response.getCategory());
    }

    @Test
    void updateEvent_whenEventNotFound_throwsNotFound() {
        UpdateEventRequest request = buildValidUpdateRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.updateEvent(null, 99999L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateEvent_whenEventCancelled_throwsConflict() {
        Event event = eventRepository.save(buildEventWithStatus(defaultLocation, "Cancelled Show", Category.CONCERT,
                OffsetDateTime.now().plusDays(10), EventStatus.CANCELLED));

        Long eventId = event.getEventId();

        UpdateEventRequest request = buildValidUpdateRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.updateEvent(null, eventId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateEvent_whenEventPast_throwsConflict() {
        Event event = eventRepository.save(buildEventWithStatus(defaultLocation, "Past Show", Category.CONCERT,
                OffsetDateTime.parse("2024-01-01T10:00:00Z"), EventStatus.PAST));

        Long eventId = event.getEventId();

        UpdateEventRequest request = buildValidUpdateRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.updateEvent(null, eventId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateEvent_whenTimeChanges_sendsNotificationEmails() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Time Change Show", Category.CONCERT,
                OffsetDateTime.now().plusDays(10)));
        User user = userRepository.save(new User("Fan", "fan@test.com", "5140001099",
                "Pass1!", UserRole.CUSTOMER));

        reservationRepository.save(buildReservation(user, event, ReservationStatus.PENDING, "79.99"));

        UpdateEventRequest request = buildValidUpdateRequest();
        request.setStartDateTime(OffsetDateTime.now().plusDays(15));
        request.setEndDateTime(OffsetDateTime.now().plusDays(15).plusHours(2));

        eventService.updateEvent(null, event.getEventId(), request);

        verify(reservationEmailService).sendEventTimeUpdatedConfirmation(
                eq("fan@test.com"), any(), any(), any(), any());
    }

    @Test
    void updateEvent_whenTimeUnchanged_doesNotSendNotificationEmails() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(10);
        Event event = eventRepository.save(buildEvent(defaultLocation, "No Change Show", Category.CONCERT, start));

        UpdateEventRequest request = buildValidUpdateRequest();
        request.setStartDateTime(start);
        request.setEndDateTime(start.plusHours(2));

        eventService.updateEvent(null, event.getEventId(), request);

        verify(reservationEmailService, never()).sendEventTimeUpdatedConfirmation(any(), any(), any(), any(), any());
    }

    @Test
    void cancelEvent_cancelsAllActiveReservations() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Big Show", Category.CONCERT,
                OffsetDateTime.now().plusDays(10)));
        User user = userRepository.save(new User("Fan", "fan@test.com", "5140001099",
                "Pass1!", UserRole.CUSTOMER));
        Reservation reservation = reservationRepository.save(buildReservation(user, event, ReservationStatus.PENDING, "79.99"));

        eventService.cancelEvent(null, event.getEventId());

        Reservation updated = reservationRepository.findById(reservation.getReservationId()).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void cancelEvent_voidsAllTickets() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Big Show", Category.CONCERT,
                OffsetDateTime.now().plusDays(10)));
        User user = userRepository.save(new User("Fan", "fan@test.com", "5140001099",
                "Pass1!", UserRole.CUSTOMER));
        Reservation reservation = reservationRepository.save(buildReservation(user, event, ReservationStatus.PENDING, "79.99"));
        Ticket ticket = ticketRepository.save(new Ticket(reservation, new BigDecimal("79.99")));

        eventService.cancelEvent(null, event.getEventId());

        Ticket updated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertEquals(TicketStatus.VOIDED, updated.getStatus());
    }

    @Test
    void cancelEvent_sendsEmailToEachAffectedUser() {
        Event event = eventRepository.save(buildEvent(defaultLocation, "Big Show", Category.CONCERT,
                OffsetDateTime.now().plusDays(10)));
        User user = userRepository.save(new User("Fan", "fan@test.com", "5140001099",
                "Pass1!", UserRole.CUSTOMER));
        reservationRepository.save(buildReservation(user, event, ReservationStatus.PENDING, "79.99"));

        eventService.cancelEvent(null, event.getEventId());

        verify(reservationEmailService).sendEventCancellationConfirmation(
                eq("fan@test.com"), any(), any());
    }

    private Event buildEvent(Location location, String title, Category category, OffsetDateTime startDateTime) {
        return new Event(location, title, startDateTime, startDateTime.plusHours(2),
                category, new BigDecimal("49.99"), EventStatus.SCHEDULED);
    }

    private Event buildEventWithStatus(Location location, String title, Category category,
                                       OffsetDateTime startDateTime, EventStatus status) {
        return new Event(location, title, startDateTime, startDateTime.plusHours(2),
                category, new BigDecimal("49.99"), status);
    }

    private Reservation buildReservation(User user, Event event, ReservationStatus status, String totalPrice) {
        return new Reservation(user, event, OffsetDateTime.parse("2026-05-01T12:00:00Z"),
                status, new BigDecimal(totalPrice));
    }

    private UpdateEventRequest buildValidUpdateRequest() {
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Valid Title");
        request.setStartDateTime(OffsetDateTime.now().plusDays(10));
        request.setEndDateTime(OffsetDateTime.now().plusDays(10).plusHours(2));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("49.99"));
        request.setLocationId(defaultLocation.getLocationId());
        return request;
    }
}
