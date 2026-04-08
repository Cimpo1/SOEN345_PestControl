package com.pestcontrol.backend.service;

import com.pestcontrol.backend.api.dto.ReservationResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.TicketStatus;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservationService {
    private static final String REGISTERED = "REGISTERED";
    private static final String PASSED = "PASSED";
    private static final String CANCELLED = "CANCELLED";
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final ReservationEmailService reservationEmailService;
    private final Clock clock;

    @Autowired
    public ReservationService(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            TicketRepository ticketRepository,
            ReservationEmailService reservationEmailService) {
        this(
                reservationRepository,
                userRepository,
                eventRepository,
                ticketRepository,
                reservationEmailService,
                Clock.systemUTC());
    }

    public ReservationService(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            TicketRepository ticketRepository,
            ReservationEmailService reservationEmailService,
            Clock clock) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.reservationEmailService = reservationEmailService;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse reserve(Long userId, Long eventId, Integer quantity) {
        if (eventId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId is required");
        }

        int validatedQuantity = quantity == null ? 1 : quantity;
        if (validatedQuantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be at least 1");
        }

        User user = getUser(userId);
        Event event = getEvent(eventId);

        if (isEventUnavailable(event)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not available for reservation");
        }

        List<Reservation> activeReservations = reservationRepository.findByUserAndEventAndStatusIn(
                user,
                event,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        if (!activeReservations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already registered to this event");
        }

        Reservation reservation = new Reservation(
                user,
                event,
                OffsetDateTime.now(clock),
                ReservationStatus.CONFIRMED,
                event.getBasePrice().multiply(java.math.BigDecimal.valueOf(validatedQuantity)));

        Reservation saved = reservationRepository.save(reservation);

        List<Ticket> ticketsToCreate = new ArrayList<>();
        for (int i = 0; i < validatedQuantity; i++) {
            ticketsToCreate.add(new Ticket(saved, event.getBasePrice()));
        }
        List<Ticket> savedTickets = ticketRepository.saveAll(ticketsToCreate);
        saved.getTickets().addAll(savedTickets);

        if (saved.getUser().getEmail() != null) {
            reservationEmailService.sendReservationConfirmation(saved.getUser().getEmail(), saved, savedTickets);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getCurrentReservations(Long userId) {
        User user = getUser(userId);

        return reservationRepository.findByUserAndStatusOrderByCreationDateDesc(user, ReservationStatus.CONFIRMED)
                .stream()
                .filter(reservation -> !isPassed(reservation.getEvent()))
                .sorted(Comparator.comparing((Reservation reservation) -> reservation.getEvent().getStartDateTime()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getInteractedEvents(Long userId) {
        User user = getUser(userId);

        return reservationRepository.findByUserOrderByCreationDateDesc(user).stream()
                .sorted(Comparator.comparing((Reservation reservation) -> reservation.getEvent().getStartDateTime())
                        .reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse cancel(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        List<Ticket> tickets = ticketRepository.findByReservation(saved);
        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.VOIDED);
        }
        if (!tickets.isEmpty()) {
            ticketRepository.saveAll(tickets);
        }

        if (saved.getUser().getEmail() != null) {
            reservationEmailService.sendCancellationConfirmation(saved.getUser().getEmail(), saved, tickets);
        }
        return toResponse(saved);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private boolean isEventUnavailable(Event event) {
        return event.getStatus() == EventStatus.CANCELLED || isPassed(event);
    }

    private boolean isPassed(Event event) {
        return event.getStatus() == EventStatus.PAST || !event.getEndDateTime().isAfter(OffsetDateTime.now(clock));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        String interactionStatus;

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            interactionStatus = CANCELLED;
        } else if (isPassed(reservation.getEvent())) {
            interactionStatus = PASSED;
        } else {
            interactionStatus = REGISTERED;
        }

        int ticketCount = (int) ticketRepository.countByReservation(reservation);

        return new ReservationResponse(reservation, interactionStatus, ticketCount);
    }

}
