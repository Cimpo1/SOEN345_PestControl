package com.pestcontrol.backend.unit.service;

import com.pestcontrol.backend.api.dto.EventResponse;
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
import com.pestcontrol.backend.infrastructure.repositories.LocationRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.service.EventService;
import com.pestcontrol.backend.service.ReservationEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ReservationEmailService reservationEmailService;

    @InjectMocks
    private EventService eventService;

    private Event montrealConcert;
    private Event lavalSports;
    private Event cancelledEvent;

    @BeforeEach
    void setUp() {
        when(eventRepository.findByStatus(EventStatus.SCHEDULED)).thenReturn(List.of());

        Location montreal = new Location("Bell Centre", "1 Arena Way", "Montreal", "QC", "H1A1A1");
        montreal.setLocationId(1L);

        Location laval = new Location("Place Bell", "2 Arena Way", "Laval", "QC", "H2B2B2");
        laval.setLocationId(2L);

        montrealConcert = new Event(
                montreal,
                "Summer Jazz Festival",
                OffsetDateTime.parse("2026-07-10T19:00:00Z"),
                OffsetDateTime.parse("2026-07-10T22:00:00Z"),
                Category.CONCERT,
                new BigDecimal("59.99"),
                EventStatus.SCHEDULED);
        montrealConcert.setEventId(10L);

        lavalSports = new Event(
                laval,
                "City Soccer Finals",
                OffsetDateTime.parse("2026-08-15T18:00:00Z"),
                OffsetDateTime.parse("2026-08-15T21:00:00Z"),
                Category.SPORTS,
                new BigDecimal("39.99"),
                EventStatus.SCHEDULED);
        lavalSports.setEventId(11L);

        cancelledEvent = new Event(
                montreal,
                "Cancelled Comedy Night",
                OffsetDateTime.parse("2026-09-20T18:00:00Z"),
                OffsetDateTime.parse("2026-09-20T20:00:00Z"),
                Category.COMEDY,
                new BigDecimal("29.99"),
                EventStatus.CANCELLED);
        cancelledEvent.setEventId(12L);
    }

    @Test
    void getEvents_whenNoFilters_returnsOnlyScheduledEvents() {
        when(eventRepository.findAll()).thenReturn(List.of(montrealConcert, lavalSports, cancelledEvent));

        List<EventResponse> result = eventService.getEvents(null, null, null, null, List.of());

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getEventId());
        assertEquals(11L, result.get(1).getEventId());
    }

    @Test
    void getEvents_whenAllFiltersProvided_appliesAndLogic() {
        when(eventRepository.findAll()).thenReturn(List.of(montrealConcert, lavalSports));

        List<EventResponse> result = eventService.getEvents(
                "jazz",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-31"),
                "montreal",
                List.of(Category.CONCERT));

        assertEquals(1, result.size());
        assertEquals("Summer Jazz Festival", result.get(0).getTitle());
    }

    @Test
    void getEvents_whenEndDateBeforeStartDate_throwsBadRequest() {
        LocalDate startDate = LocalDate.parse("2026-08-01");
        LocalDate endDate = LocalDate.parse("2026-07-01");
        List<Category> categories = List.of();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.getEvents(
                        null,
                        startDate,
                        endDate,
                        null,
                        categories));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getEventById_whenEventExists_returnsEventResponse() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(montrealConcert));

        EventResponse result = eventService.getEventById(10L);

        assertEquals(10L, result.getEventId());
        assertEquals("Bell Centre", result.getLocation().getName());
    }

    @Test
    void getEventById_whenMissing_throwsNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> eventService.getEventById(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void cancelEvent_whenScheduled_cascadesReservationAndTicketUpdatesAndSendsEmails() {
        User customerWithEmail = new User("Alice", "alice@example.com", "5141112222", "hash", UserRole.CUSTOMER);
        customerWithEmail.setUserId(50L);
        User customerWithoutEmail = new User("Bob", null, "5143334444", "hash", UserRole.CUSTOMER);
        customerWithoutEmail.setUserId(51L);

        Reservation confirmedReservation = new Reservation(
                customerWithEmail,
                montrealConcert,
                OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));
        confirmedReservation.setReservationId(300L);

        Reservation alreadyCancelledReservation = new Reservation(
                customerWithoutEmail,
                montrealConcert,
                OffsetDateTime.parse("2026-07-01T11:00:00Z"),
                ReservationStatus.CANCELLED,
                new BigDecimal("39.99"));
        alreadyCancelledReservation.setReservationId(301L);

        Ticket ticketOne = new Ticket(confirmedReservation, new BigDecimal("59.99"));
        ticketOne.setTicketId(1L);
        Ticket ticketTwo = new Ticket(alreadyCancelledReservation, new BigDecimal("39.99"));
        ticketTwo.setTicketId(2L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(montrealConcert));
        when(eventRepository.save(montrealConcert)).thenReturn(montrealConcert);
        when(reservationRepository.findByEvent(montrealConcert))
                .thenReturn(List.of(confirmedReservation, alreadyCancelledReservation));
        when(ticketRepository.findByReservation(confirmedReservation)).thenReturn(List.of(ticketOne));
        when(ticketRepository.findByReservation(alreadyCancelledReservation)).thenReturn(List.of(ticketTwo));

        EventResponse response = eventService.cancelEvent(999L, 10L);

        assertEquals(EventStatus.CANCELLED, montrealConcert.getStatus());
        assertEquals(EventStatus.CANCELLED, response.getStatus());
        assertEquals(ReservationStatus.CANCELLED, confirmedReservation.getStatus());
        assertEquals(TicketStatus.VOIDED, ticketOne.getStatus());
        assertEquals(TicketStatus.VOIDED, ticketTwo.getStatus());

        verify(eventRepository).save(montrealConcert);
        verify(reservationRepository).save(confirmedReservation);
        verify(reservationRepository, never()).save(alreadyCancelledReservation);
        verify(ticketRepository).saveAll(List.of(ticketOne));
        verify(ticketRepository).saveAll(List.of(ticketTwo));
        verify(reservationEmailService).sendEventCancellationConfirmation("alice@example.com", confirmedReservation,
                List.of(ticketOne));
        verify(reservationEmailService, times(1)).sendEventCancellationConfirmation(any(), any(), any());
    }

    @Test
    void cancelEvent_whenEventAlreadyCancelled_throwsConflict() {
        when(eventRepository.findById(12L)).thenReturn(Optional.of(cancelledEvent));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.cancelEvent(999L, 12L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateEvent_whenTimeChanges_notifiesActiveReservationsOnly() {
        User activeCustomer = new User("Alice", "alice@example.com", "5141112222", "hash", UserRole.CUSTOMER);
        activeCustomer.setUserId(50L);
        User cancelledCustomer = new User("Bob", "bob@example.com", "5143334444", "hash", UserRole.CUSTOMER);
        cancelledCustomer.setUserId(51L);

        Reservation activeReservation = new Reservation(
                activeCustomer,
                montrealConcert,
                OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));
        activeReservation.setReservationId(300L);

        Reservation cancelledReservation = new Reservation(
                cancelledCustomer,
                montrealConcert,
                OffsetDateTime.parse("2026-07-01T11:00:00Z"),
                ReservationStatus.CANCELLED,
                new BigDecimal("59.99"));
        cancelledReservation.setReservationId(301L);

        Ticket activeTicket = new Ticket(activeReservation, new BigDecimal("59.99"));
        activeTicket.setTicketId(900L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(montrealConcert));
        when(eventRepository.save(montrealConcert)).thenReturn(montrealConcert);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(montrealConcert.getLocation()));
        when(reservationRepository.findByEvent(montrealConcert))
                .thenReturn(List.of(activeReservation, cancelledReservation));
        when(ticketRepository.findByReservation(activeReservation)).thenReturn(List.of(activeTicket));

        var request = new com.pestcontrol.backend.api.dto.UpdateEventRequest();
        request.setTitle("Summer Jazz Festival");
        request.setStartDateTime(OffsetDateTime.parse("2026-07-10T20:00:00Z"));
        request.setEndDateTime(OffsetDateTime.parse("2026-07-10T23:00:00Z"));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("59.99"));
        request.setLocationId(1L);

        EventResponse response = eventService.updateEvent(999L, 10L, request);

        assertEquals(OffsetDateTime.parse("2026-07-10T20:00:00Z"), response.getStartDateTime());
        verify(reservationEmailService).sendEventTimeUpdatedConfirmation(
                eq("alice@example.com"),
                eq(activeReservation),
                eq(List.of(activeTicket)),
                eq("2026-07-10 19:00 UTC"),
                eq("2026-07-10 22:00 UTC"));
        verify(reservationEmailService, never()).sendEventTimeUpdatedConfirmation(
                eq("bob@example.com"),
                any(),
                any(),
                any(),
                any());
    }
}