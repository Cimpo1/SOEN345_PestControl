package com.pestcontrol.backend.integration.controller;

import com.pestcontrol.backend.api.ReservationController;
import com.pestcontrol.backend.api.dto.CreateReservationRequest;
import com.pestcontrol.backend.api.dto.ReservationResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.JWTService;
import com.pestcontrol.backend.service.ReservationEmailService;
import com.pestcontrol.backend.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerIntegrationTest {

    @Mock ReservationRepository reservationRepository;
    @Mock UserRepository userRepository;
    @Mock EventRepository eventRepository;
    @Mock TicketRepository ticketRepository;
    @Mock ReservationEmailService reservationEmailService;

    private ReservationController reservationController;

    private String userToken;
    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                userRepository,
                eventRepository,
                ticketRepository,
                reservationEmailService);
        reservationController = new ReservationController(reservationService);

        testUser  = buildUser(2L, UserRole.CUSTOMER);
        userToken = "Bearer " + JWTService.generateToken(testUser);
        testEvent = buildEvent(10L, EventStatus.SCHEDULED);
    }

    @Test
    void reserve_withValidTokenAndAvailableEvent_returns201() {
        Reservation saved = buildReservation(1L, testUser, testEvent, ReservationStatus.CONFIRMED);

        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(testEvent));
        when(reservationRepository.findByUserAndEventAndStatusIn(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);
        when(ticketRepository.saveAll(any())).thenReturn(Collections.emptyList());
        when(ticketRepository.countByReservation(any())).thenReturn(1L);

        ResponseEntity<ReservationResponse> response =
                reservationController.reserve(userToken, buildReservationRequest(10L, 1));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(reservationRepository).save(any(Reservation.class));
        verify(ticketRepository).saveAll(any());
    }

    @Test
    void reserve_withMissingAuthHeader_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.reserve(null, buildReservationRequest(10L, 1)));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_withInvalidToken_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.reserve(
                        "Bearer not.a.valid.jwt",
                        buildReservationRequest(10L, 1)));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_whenAlreadyRegistered_throws409() {
        Reservation existing = buildReservation(1L, testUser, testEvent, ReservationStatus.CONFIRMED);

        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(testEvent));
        when(reservationRepository.findByUserAndEventAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.reserve(userToken, buildReservationRequest(10L, 1)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_forCancelledEvent_throws400() {
        Event cancelled = buildEvent(10L, EventStatus.CANCELLED);

        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(cancelled));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.reserve(userToken, buildReservationRequest(10L, 1)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_withQuantityZero_throws400() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.reserve(userToken, buildReservationRequest(10L, 0)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_withUnknownEventId_throws404() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.reserve(userToken, buildReservationRequest(10L, 1)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getCurrentReservations_withValidToken_returns200WithList() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUserAndStatusOrderByCreationDateDesc(
                any(User.class), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<ReservationResponse>> response =
                reservationController.getCurrentReservations(userToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationRepository).findByUserAndStatusOrderByCreationDateDesc(
                any(User.class), eq(ReservationStatus.CONFIRMED));
    }

    @Test
    void getCurrentReservations_withMissingAuthHeader_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.getCurrentReservations(null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(reservationRepository, never())
                .findByUserAndStatusOrderByCreationDateDesc(any(), any());
    }

    @Test
    void getInteractedEvents_withValidToken_returns200WithList() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUserOrderByCreationDateDesc(any(User.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<ReservationResponse>> response =
                reservationController.getInteractedEvents(userToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationRepository).findByUserOrderByCreationDateDesc(any(User.class));
    }

    @Test
    void getInteractedEvents_withMissingAuthHeader_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.getInteractedEvents(null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(reservationRepository, never()).findByUserOrderByCreationDateDesc(any());
    }

    @Test
    void cancelReservation_ownedByCurrentUser_returns200() {
        Reservation reservation = buildReservation(5L, testUser, testEvent, ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(ticketRepository.findByReservation(any(Reservation.class)))
                .thenReturn(Collections.emptyList());
        when(ticketRepository.countByReservation(any(Reservation.class))).thenReturn(0L);

        ResponseEntity<ReservationResponse> response =
                reservationController.cancelReservation(userToken, 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void cancelReservation_alreadyCancelled_throws409() {
        Reservation reservation = buildReservation(5L, testUser, testEvent, ReservationStatus.CANCELLED);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.cancelReservation(userToken, 5L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_belongingToAnotherUser_throws403() {
        User otherUser = buildUser(999L, UserRole.CUSTOMER);
        Reservation reservation = buildReservation(5L, otherUser, testEvent, ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.cancelReservation(userToken, 5L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_notFound_throws404() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.cancelReservation(userToken, 999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void cancelReservation_withMissingAuthHeader_throws401() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.cancelReservation(null, 5L));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(reservationRepository, never()).findById(any());
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

    private Reservation buildReservation(Long id, User user, Event event, ReservationStatus status) {
        Reservation reservation = new Reservation(
                user, event, OffsetDateTime.now(), status, event.getBasePrice());
        reservation.setReservationId(id);
        return reservation;
    }

    private CreateReservationRequest buildReservationRequest(Long eventId, int quantity) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setEventId(eventId);
        request.setQuantity(quantity);
        return request;
    }
}