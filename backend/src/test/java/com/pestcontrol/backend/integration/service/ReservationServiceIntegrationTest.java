package com.pestcontrol.backend.integration.service;

import com.pestcontrol.backend.api.dto.ReservationResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
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
import com.pestcontrol.backend.service.ReservationEmailService;
import com.pestcontrol.backend.service.ReservationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TicketRepository ticketRepository;

    @MockitoBean
    private ReservationEmailService reservationEmailService;

    private ReservationService reservationService;
    private User defaultUser;
    private Location defaultLocation;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                userRepository,
                eventRepository,
                ticketRepository,
                reservationEmailService,
                FIXED_CLOCK);

        defaultLocation = new Location("Test Venue", "1 Test St", "Montreal", "QC", "H1A 1A1");
        entityManager.persist(defaultLocation);
        entityManager.flush();

        defaultUser = userRepository.save(
                new User("Test User", "test@test.com", "5140000001", "hash", UserRole.CUSTOMER));
    }

    @Test
    void reserve_withValidEventAndQuantity_createsReservationWithConfirmedStatus() {
        Event event = savedFutureEvent("Jazz Night", EventStatus.SCHEDULED);

        ReservationResponse response = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 2);

        assertEquals(ReservationStatus.CONFIRMED.name(), response.getReservationStatus());
        assertEquals("REGISTERED", response.getInteractionStatus());
        assertEquals(2, response.getTicketCount());
    }
    @Test
    void reserve_withNullQuantity_defaultsToOneTicket() {
        Event event = savedFutureEvent("Default Qty Event", EventStatus.SCHEDULED);

        ReservationResponse response = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), null);

        assertEquals(1, response.getTicketCount());
    }

    @Test
    void reserve_withValidData_sendsConfirmationEmail() {
        Event event = savedFutureEvent("Email Event", EventStatus.SCHEDULED);

        reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);

        verify(reservationEmailService, times(1))
                .sendReservationConfirmation(eq("test@test.com"), any(), any());
    }

    @Test
    void reserve_withNullEventId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), null, 1));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reserve_withQuantityZero_throwsBadRequest() {
        Event event = savedFutureEvent("Zero Qty Event", EventStatus.SCHEDULED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 0));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reserve_whenUserNotFound_throwsUnauthorized() {
        Event event = savedFutureEvent("Ghost User Event", EventStatus.SCHEDULED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(99999L, event.getEventId(), 1));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void reserve_whenEventNotFound_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), 99999L, 1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void reserve_whenEventIsCancelled_throwsBadRequest() {
        Event event = savedFutureEvent("Cancelled Event", EventStatus.CANCELLED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reserve_whenEventEndDateTimeIsInThePast_throwsBadRequest() {
        Event event = savedEventWithDates("Past Event",
                "2025-06-01T18:00:00Z", "2025-06-01T22:00:00Z", EventStatus.SCHEDULED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reserve_whenEventStatusIsPast_throwsBadRequest() {
        Event event = savedFutureEvent("Status Past Event", EventStatus.PAST);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void reserve_whenAlreadyReserved_throwsConflict() {
        Event event = savedFutureEvent("Duplicate Event", EventStatus.SCHEDULED);
        reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
    @Test
    void getCurrentReservations_returnsOnlyConfirmedFutureReservations() {
        Event futureEvent = savedFutureEvent("Future Show", EventStatus.SCHEDULED);
        Event pastEvent = savedEventWithDates("Past Show",
                "2025-01-01T18:00:00Z", "2025-01-01T22:00:00Z", EventStatus.SCHEDULED);

        reservationService.reserve(defaultUser.getUserId(), futureEvent.getEventId(), 1);

        reservationRepository.save(new com.pestcontrol.backend.domain.Reservation(
                defaultUser, pastEvent,
                OffsetDateTime.parse("2024-12-01T12:00:00Z"),
                ReservationStatus.CONFIRMED,
                pastEvent.getBasePrice()));

        List<ReservationResponse> current = reservationService.getCurrentReservations(defaultUser.getUserId());

        assertEquals(1, current.size());
        assertEquals("Future Show", current.get(0).getEvent().getTitle());
    }

    @Test
    void getCurrentReservations_excludesCancelledReservations() {
        Event event = savedFutureEvent("Cancelled Show", EventStatus.SCHEDULED);
        ReservationResponse reservation = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);
        reservationService.cancel(defaultUser.getUserId(), reservation.getReservationId());

        List<ReservationResponse> current = reservationService.getCurrentReservations(defaultUser.getUserId());

        assertTrue(current.isEmpty());
    }


    @Test
    void getInteractedEvents_returnsAllReservationsRegardlessOfStatus() {
        Event eventOne = savedFutureEvent("Show One", EventStatus.SCHEDULED);
        Event eventTwo = savedFutureEvent("Show Two", EventStatus.SCHEDULED);

        ReservationResponse r1 = reservationService.reserve(defaultUser.getUserId(), eventOne.getEventId(), 1);
        reservationService.reserve(defaultUser.getUserId(), eventTwo.getEventId(), 1);
        reservationService.cancel(defaultUser.getUserId(), r1.getReservationId());

        List<ReservationResponse> interacted = reservationService.getInteractedEvents(defaultUser.getUserId());

        assertEquals(2, interacted.size());
    }
    @Test
    void cancel_withValidReservation_setsReservationStatusToCancelled() {
        Event event = savedFutureEvent("Cancel Me", EventStatus.SCHEDULED);
        ReservationResponse reserved = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);

        ReservationResponse cancelled = reservationService.cancel(defaultUser.getUserId(), reserved.getReservationId());

        assertEquals(ReservationStatus.CANCELLED.name(), cancelled.getReservationStatus());
        assertEquals("CANCELLED", cancelled.getInteractionStatus());
    }

    @Test
    void cancel_whenReservationNotFound_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancel(defaultUser.getUserId(), 99999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void cancel_whenUserDoesNotOwnReservation_throwsForbidden() {
        User otherUser = userRepository.save(
                new User("Other User", "other@test.com", "5140000002", "hash", UserRole.CUSTOMER));
        Event event = savedFutureEvent("Forbidden Event", EventStatus.SCHEDULED);
        ReservationResponse reserved = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancel(otherUser.getUserId(), reserved.getReservationId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsConflict() {
        Event event = savedFutureEvent("Double Cancel", EventStatus.SCHEDULED);
        ReservationResponse reserved = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);
        reservationService.cancel(defaultUser.getUserId(), reserved.getReservationId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancel(defaultUser.getUserId(), reserved.getReservationId()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
    @Test
    void cancel_voidsAllTickets() {
        Event event = savedFutureEvent("Voiding Event", EventStatus.SCHEDULED);
        ReservationResponse reserved = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 3);

        reservationService.cancel(defaultUser.getUserId(), reserved.getReservationId());

        var reservation = reservationRepository.findById(reserved.getReservationId()).orElseThrow();
        var tickets = ticketRepository.findByReservation(reservation);
        assertEquals(3, tickets.size());
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == TicketStatus.VOIDED));
    }

    @Test
    void cancel_sendsCancellationEmail() {
        Event event = savedFutureEvent("Email Cancel", EventStatus.SCHEDULED);
        ReservationResponse reserved = reservationService.reserve(defaultUser.getUserId(), event.getEventId(), 1);

        reservationService.cancel(defaultUser.getUserId(), reserved.getReservationId());

        verify(reservationEmailService, times(1))
                .sendCancellationConfirmation(eq("test@test.com"), any(), any());
    }


    private Event savedFutureEvent(String title, EventStatus status) {
        return savedEventWithDates(title, "2026-08-01T18:00:00Z", "2026-08-01T22:00:00Z", status);
    }

    private Event savedEventWithDates(String title, String start, String end, EventStatus status) {
        return eventRepository.save(new Event(
                defaultLocation, title,
                OffsetDateTime.parse(start),
                OffsetDateTime.parse(end),
                Category.CONCERT,
                new BigDecimal("59.99"),
                status));
    }
}
