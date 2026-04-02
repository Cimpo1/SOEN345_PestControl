package com.pestcontrol.backend.unit.service;

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
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Event upcomingEvent;
    private Event pastEvent;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T14:00:00Z"), ZoneOffset.UTC);
        reservationService = new ReservationService(reservationRepository, userRepository, eventRepository, fixedClock);

        user = new User("Customer", "customer@test.com", "5145550000", "hash", UserRole.CUSTOMER);
        user.setUserId(7L);

        Location location = new Location("Bell Centre", "1 Arena", "Montreal", "QC", "H1A1A1");

        upcomingEvent = new Event(
                location,
                "Upcoming Show",
                OffsetDateTime.parse("2026-04-03T18:00:00Z"),
                OffsetDateTime.parse("2026-04-03T21:00:00Z"),
                Category.CONCERT,
                new BigDecimal("59.99"),
                EventStatus.SCHEDULED);
        upcomingEvent.setEventId(10L);

        pastEvent = new Event(
                location,
                "Past Show",
                OffsetDateTime.parse("2026-03-25T18:00:00Z"),
                OffsetDateTime.parse("2026-03-25T21:00:00Z"),
                Category.COMEDY,
                new BigDecimal("39.99"),
                EventStatus.SCHEDULED);
        pastEvent.setEventId(11L);
    }

    @Test
    void reserve_whenValid_createsConfirmedReservation() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcomingEvent));
        when(reservationRepository.findByUserAndEventAndStatusIn(any(), any(), any())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setReservationId(99L);
            return reservation;
        });

        ReservationResponse response = reservationService.reserve(7L, 10L);

        assertEquals(99L, response.getReservationId());
        assertEquals("CONFIRMED", response.getReservationStatus());
        assertEquals("REGISTERED", response.getInteractionStatus());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(ReservationStatus.CONFIRMED, captor.getValue().getStatus());
    }

    @Test
    void reserve_whenAlreadyRegistered_throwsConflict() {
        Reservation existing = new Reservation(
                user,
                upcomingEvent,
                OffsetDateTime.parse("2026-04-01T12:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcomingEvent));
        when(reservationRepository.findByUserAndEventAndStatusIn(any(), any(), any())).thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(7L, 10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void reserve_whenEventPast_throwsBadRequest() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(11L)).thenReturn(Optional.of(pastEvent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.reserve(7L, 11L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getInteractedEvents_whenPastUpcomingAndCancelled_returnsStatusLabels() {
        Reservation r1 = new Reservation(
                user,
                pastEvent,
                OffsetDateTime.parse("2026-03-01T10:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("39.99"));
        r1.setReservationId(1L);

        Reservation r2 = new Reservation(
                user,
                upcomingEvent,
                OffsetDateTime.parse("2026-03-30T10:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));
        r2.setReservationId(2L);

        Reservation r3 = new Reservation(
                user,
                upcomingEvent,
                OffsetDateTime.parse("2026-03-31T10:00:00Z"),
                ReservationStatus.CANCELLED,
                new BigDecimal("59.99"));
        r3.setReservationId(3L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(reservationRepository.findByUserOrderByCreationDateDesc(user)).thenReturn(List.of(r1, r2, r3));

        List<ReservationResponse> result = reservationService.getInteractedEvents(7L);

        assertEquals(3, result.size());
        Set<String> statuses = result.stream().map(ReservationResponse::getInteractionStatus)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("REGISTERED", "PASSED", "CANCELLED"), statuses);
    }

    @Test
    void cancel_whenReservationBelongsToDifferentUser_throwsForbidden() {
        User anotherUser = new User("Another", "another@test.com", "5145551111", "hash", UserRole.CUSTOMER);
        anotherUser.setUserId(8L);

        Reservation reservation = new Reservation(
                anotherUser,
                upcomingEvent,
                OffsetDateTime.parse("2026-03-30T10:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));
        reservation.setReservationId(100L);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.cancel(7L, 100L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
