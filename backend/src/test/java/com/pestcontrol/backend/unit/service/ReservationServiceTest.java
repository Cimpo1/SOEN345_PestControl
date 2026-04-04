package com.pestcontrol.backend.unit.service;

import com.pestcontrol.backend.api.dto.ReservationResponse;
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
import com.pestcontrol.backend.service.ReservationEmailService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

        @Mock
        private TicketRepository ticketRepository;

        @Mock
        private ReservationEmailService reservationEmailService;

        @InjectMocks
        private ReservationService reservationService;

        private User user;
        private Event upcomingEvent;
        private Event pastEvent;

        @BeforeEach
        void setUp() {
                Clock fixedClock = Clock.fixed(Instant.parse("2026-04-02T14:00:00Z"), ZoneOffset.UTC);
                reservationService = new ReservationService(
                                reservationRepository,
                                userRepository,
                                eventRepository,
                                ticketRepository,
                                reservationEmailService,
                                fixedClock);

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
                when(ticketRepository.saveAll(any())).thenAnswer(invocation -> {
                        @SuppressWarnings("unchecked")
                        List<Ticket> tickets = (List<Ticket>) invocation.getArgument(0);
                        long id = 199L;
                        for (Ticket ticket : tickets) {
                                ticket.setTicketId(id++);
                                ticket.setStatus(TicketStatus.ISSUED);
                        }
                        return tickets;
                });

                ReservationResponse response = reservationService.reserve(7L, 10L, 2);

                assertEquals(99L, response.getReservationId());
                assertEquals("CONFIRMED", response.getReservationStatus());
                assertEquals("REGISTERED", response.getInteractionStatus());

                ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
                ArgumentCaptor<List> emailTicketsCaptor = ArgumentCaptor.forClass(List.class);
                verify(reservationRepository).save(captor.capture());
                assertEquals(ReservationStatus.CONFIRMED, captor.getValue().getStatus());
                assertEquals(new BigDecimal("119.98"), captor.getValue().getTotalPrice());
                verify(ticketRepository).saveAll(any());
                verify(reservationEmailService).sendReservationConfirmation(eq("customer@test.com"),
                                any(Reservation.class), emailTicketsCaptor.capture());
                assertEquals(2, emailTicketsCaptor.getValue().size());
        }

        @Test
        void reserve_whenUserHasNoEmail_doesNotSendConfirmationEmail() {
                user.setEmail(null);

                when(userRepository.findById(7L)).thenReturn(Optional.of(user));
                when(eventRepository.findById(10L)).thenReturn(Optional.of(upcomingEvent));
                when(reservationRepository.findByUserAndEventAndStatusIn(any(), any(), any())).thenReturn(List.of());
                when(reservationRepository.save(any())).thenAnswer(invocation -> {
                        Reservation reservation = invocation.getArgument(0);
                        reservation.setReservationId(99L);
                        return reservation;
                });
                when(ticketRepository.saveAll(any())).thenAnswer(invocation -> {
                        @SuppressWarnings("unchecked")
                        List<Ticket> tickets = (List<Ticket>) invocation.getArgument(0);
                        return tickets;
                });

                reservationService.reserve(7L, 10L, 1);

                verify(reservationEmailService, never()).sendReservationConfirmation(any(), any(), any());
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
                when(reservationRepository.findByUserAndEventAndStatusIn(any(), any(), any()))
                                .thenReturn(List.of(existing));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, 10L, 1));

                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        void reserve_whenEventPast_throwsBadRequest() {
                when(userRepository.findById(7L)).thenReturn(Optional.of(user));
                when(eventRepository.findById(11L)).thenReturn(Optional.of(pastEvent));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, 11L, 1));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void reserve_whenEventIdMissing_throwsBadRequest() {
                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, null, 1));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void reserve_whenUserMissing_throwsUnauthorized() {
                when(userRepository.findById(7L)).thenReturn(Optional.empty());

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, 10L, 1));

                assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void reserve_whenEventMissing_throwsNotFound() {
                when(userRepository.findById(7L)).thenReturn(Optional.of(user));
                when(eventRepository.findById(10L)).thenReturn(Optional.empty());

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, 10L, 1));

                assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void reserve_whenEventCancelled_throwsBadRequest() {
                Event cancelledEvent = new Event(
                                upcomingEvent.getLocation(),
                                "Cancelled Show",
                                OffsetDateTime.parse("2026-04-10T18:00:00Z"),
                                OffsetDateTime.parse("2026-04-10T21:00:00Z"),
                                Category.CONCERT,
                                new BigDecimal("59.99"),
                                EventStatus.CANCELLED);
                cancelledEvent.setEventId(12L);

                when(userRepository.findById(7L)).thenReturn(Optional.of(user));
                when(eventRepository.findById(12L)).thenReturn(Optional.of(cancelledEvent));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, 12L, 1));

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

        @Test
        void getCurrentReservations_whenMixedEvents_returnsOnlyUpcomingSorted() {
                Reservation upcomingLater = new Reservation(
                                user,
                                upcomingEvent,
                                OffsetDateTime.parse("2026-03-28T10:00:00Z"),
                                ReservationStatus.CONFIRMED,
                                new BigDecimal("59.99"));
                upcomingLater.setReservationId(20L);

                Event upcomingSooner = new Event(
                                upcomingEvent.getLocation(),
                                "Sooner Show",
                                OffsetDateTime.parse("2026-04-02T20:00:00Z"),
                                OffsetDateTime.parse("2026-04-02T23:00:00Z"),
                                Category.SPORTS,
                                new BigDecimal("49.99"),
                                EventStatus.SCHEDULED);
                upcomingSooner.setEventId(21L);

                Reservation upcomingSoonerReservation = new Reservation(
                                user,
                                upcomingSooner,
                                OffsetDateTime.parse("2026-03-29T10:00:00Z"),
                                ReservationStatus.CONFIRMED,
                                new BigDecimal("49.99"));
                upcomingSoonerReservation.setReservationId(21L);

                Reservation pastReservation = new Reservation(
                                user,
                                pastEvent,
                                OffsetDateTime.parse("2026-03-01T10:00:00Z"),
                                ReservationStatus.CONFIRMED,
                                new BigDecimal("39.99"));
                pastReservation.setReservationId(22L);

                when(userRepository.findById(7L)).thenReturn(Optional.of(user));
                when(reservationRepository.findByUserAndStatusOrderByCreationDateDesc(user,
                                ReservationStatus.CONFIRMED))
                                .thenReturn(List.of(upcomingLater, pastReservation, upcomingSoonerReservation));

                List<ReservationResponse> result = reservationService.getCurrentReservations(7L);

                assertEquals(2, result.size());
                assertEquals(upcomingSooner.getTitle(), result.get(0).getEvent().getTitle());
                assertEquals(upcomingEvent.getTitle(), result.get(1).getEvent().getTitle());
        }

        @Test
        void cancel_whenReservationMissing_throwsNotFound() {
                when(reservationRepository.findById(100L)).thenReturn(Optional.empty());

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.cancel(7L, 100L));

                assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void cancel_whenReservationAlreadyCancelled_throwsConflict() {
                Reservation reservation = new Reservation(
                                user,
                                upcomingEvent,
                                OffsetDateTime.parse("2026-03-30T10:00:00Z"),
                                ReservationStatus.CANCELLED,
                                new BigDecimal("59.99"));
                reservation.setReservationId(101L);

                when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.cancel(7L, 101L));

                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        void cancel_whenOwnedAndConfirmed_updatesStatusToCancelled() {
                Reservation reservation = new Reservation(
                                user,
                                upcomingEvent,
                                OffsetDateTime.parse("2026-03-30T10:00:00Z"),
                                ReservationStatus.CONFIRMED,
                                new BigDecimal("59.99"));
                reservation.setReservationId(102L);

                Ticket ticket = new Ticket(reservation, new BigDecimal("59.99"));
                ticket.setTicketId(301L);
                ticket.setStatus(TicketStatus.ISSUED);

                when(reservationRepository.findById(102L)).thenReturn(Optional.of(reservation));
                when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
                when(ticketRepository.findByReservation(reservation)).thenReturn(List.of(ticket));
                when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

                ReservationResponse response = reservationService.cancel(7L, 102L);

                assertNotNull(response);
                assertEquals("CANCELLED", response.getReservationStatus());
                assertEquals("CANCELLED", response.getInteractionStatus());
                verify(ticketRepository).findByReservation(reservation);
                verify(ticketRepository).saveAll(any());
                assertEquals(TicketStatus.VOIDED, ticket.getStatus());
                verify(reservationEmailService).sendCancellationConfirmation(eq("customer@test.com"),
                                any(Reservation.class), any());
        }

        @Test
        void cancel_whenNoTickets_found_skipsTicketSaveButSendsEmail() {
                Reservation reservation = new Reservation(
                                user,
                                upcomingEvent,
                                OffsetDateTime.parse("2026-03-30T10:00:00Z"),
                                ReservationStatus.CONFIRMED,
                                new BigDecimal("59.99"));
                reservation.setReservationId(109L);

                when(reservationRepository.findById(109L)).thenReturn(Optional.of(reservation));
                when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
                when(ticketRepository.findByReservation(reservation)).thenReturn(List.of());

                ReservationResponse response = reservationService.cancel(7L, 109L);

                assertEquals("CANCELLED", response.getReservationStatus());
                verify(ticketRepository, never()).saveAll(any());
                verify(reservationEmailService).sendCancellationConfirmation(eq("customer@test.com"),
                                any(Reservation.class), any());
        }

        @Test
        void reserve_whenQuantityInvalid_throwsBadRequest() {
                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> reservationService.reserve(7L, 10L, 0));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
}
