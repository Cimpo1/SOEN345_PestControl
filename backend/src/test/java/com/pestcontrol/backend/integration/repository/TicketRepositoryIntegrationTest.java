package com.pestcontrol.backend.integration.repository;

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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@DataJpaTest
class TicketRepositoryIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByReservation_whenTicketsExist_returnsOnlyReservationTickets() {
        User user = userRepository.save(buildUser("tickets.one@test.com", "5145553001"));
        Event event = eventRepository
                .save(buildEvent(persistLocation("Ticket Venue"), "Ticket Event", Category.CONCERT));

        Reservation reservationOne = reservationRepository
                .save(buildReservation(user, event, ReservationStatus.CONFIRMED, "90.00"));
        Reservation reservationTwo = reservationRepository
                .save(buildReservation(user, event, ReservationStatus.CONFIRMED, "91.00"));

        Ticket ticketOne = ticketRepository.save(new Ticket(reservationOne, new BigDecimal("90.00")));
        Ticket ticketTwo = ticketRepository.save(new Ticket(reservationOne, new BigDecimal("95.00")));
        Ticket ticketThree = ticketRepository.save(new Ticket(reservationTwo, new BigDecimal("91.00")));

        List<Ticket> tickets = ticketRepository.findByReservation(reservationOne);

        assertEquals(2, tickets.size());
        assertEquals(TicketStatus.ISSUED, ticketOne.getStatus());
        assertEquals(TicketStatus.ISSUED, ticketTwo.getStatus());
        assertEquals(TicketStatus.ISSUED, ticketThree.getStatus());
    }

    @Test
    void save_whenStatusUpdated_persistsVoidedTicket() {
        User user = userRepository.save(buildUser("tickets.two@test.com", "5145553002"));
        Event event = eventRepository
                .save(buildEvent(persistLocation("Ticket Venue 2"), "Ticket Event 2", Category.SPORTS));
        Reservation reservation = reservationRepository
                .save(buildReservation(user, event, ReservationStatus.CONFIRMED, "120.00"));

        Ticket ticket = ticketRepository.save(new Ticket(reservation, new BigDecimal("120.00")));
        ticket.setStatus(TicketStatus.VOIDED);
        ticketRepository.save(ticket);

        Ticket reloaded = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertEquals(TicketStatus.VOIDED, reloaded.getStatus());
    }

    private Location persistLocation(String name) {
        Location location = new Location(name, "789 Main St", "Montreal", "QC", "H3C3C3");
        entityManager.persist(location);
        entityManager.flush();
        return location;
    }

    private User buildUser(String email, String phoneNumber) {
        return new User("Ticket User", email, phoneNumber, "hashed-password", UserRole.CUSTOMER);
    }

    private Event buildEvent(Location location, String title, Category category) {
        OffsetDateTime start = OffsetDateTime.parse("2026-12-01T18:00:00Z");
        return new Event(
                location,
                title,
                start,
                start.plusHours(2),
                category,
                new BigDecimal("59.99"),
                EventStatus.SCHEDULED);
    }

    private Reservation buildReservation(User user, Event event, ReservationStatus status, String totalPrice) {
        return new Reservation(
                user,
                event,
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                status,
                new BigDecimal(totalPrice));
    }
}
