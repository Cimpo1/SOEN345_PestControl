package com.pestcontrol.backend.integration.repository;

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
import com.pestcontrol.backend.service.ReservationEmailService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
class ReservationRepositoryIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ReservationEmailService reservationEmailService;

    private Location defaultLocation;

    @BeforeEach
    void setUp() {

        defaultLocation = new Location("Test Venue", "1 Test St", "Montreal", "QC", "H1A 1A1");
        entityManager.persist(defaultLocation);
        entityManager.flush();
    }

    @Test
    void findByUser_whenReservationsExist_returnsOnlyUserReservations() {
        User userOne = userRepository.save(buildUser("user.one@test.com", "5145552001"));
        User userTwo = userRepository.save(buildUser("user.two@test.com", "5145552002"));
        Event event = eventRepository.save(buildEvent(persistLocation("Reservation Hall"), "Music Night", Category.CONCERT));

        reservationRepository.save(buildReservation(userOne, event, ReservationStatus.PENDING, "79.99"));
        reservationRepository.save(buildReservation(userTwo, event, ReservationStatus.PENDING, "89.99"));

        var reservations = reservationRepository.findByUser(userOne);

        assertEquals(1, reservations.size());
        assertEquals("user.one@test.com", reservations.get(0).getUser().getEmail());
    }

    @Test
    void findByEvent_whenReservationsExist_returnsOnlyEventReservations() {
        User user = userRepository.save(buildUser("event.user@test.com", "5145552003"));
        Event eventOne = eventRepository.save(buildEvent(persistLocation("Hall A"), "Event A", Category.FAMILY));
        Event eventTwo = eventRepository.save(buildEvent(persistLocation("Hall B"), "Event B", Category.FAMILY));

        reservationRepository.save(buildReservation(user, eventOne, ReservationStatus.CONFIRMED, "69.99"));
        reservationRepository.save(buildReservation(user, eventTwo, ReservationStatus.CONFIRMED, "99.99"));

        var reservations = reservationRepository.findByEvent(eventOne);

        assertEquals(1, reservations.size());
        assertEquals("Event A", reservations.get(0).getEvent().getTitle());
    }

    @Test
    void findByUserAndStatus_whenReservationsExist_returnsOnlyMatchingReservations() {
        User user = userRepository.save(buildUser("status.user@test.com", "5145552004"));
        Event event = eventRepository.save(buildEvent(persistLocation("Status Venue"), "Status Event", Category.SPORTS));

        reservationRepository.save(buildReservation(user, event, ReservationStatus.PENDING, "49.99"));
        reservationRepository.save(buildReservation(user, event, ReservationStatus.CONFIRMED, "59.99"));

        var reservations = reservationRepository.findByUserAndStatus(user, ReservationStatus.CONFIRMED);

        assertEquals(1, reservations.size());
        assertEquals(ReservationStatus.CONFIRMED, reservations.get(0).getStatus());
    }

    @Test
    void countByEvent_whenReservationsExist_returnsCorrectCount() {
        User userOne = userRepository.save(buildUser("count.one@test.com", "5145552005"));
        User userTwo = userRepository.save(buildUser("count.two@test.com", "5145552006"));
        Event targetEvent = eventRepository.save(buildEvent(persistLocation("Count Venue"), "Counted Event", Category.COMEDY));
        Event otherEvent = eventRepository.save(buildEvent(persistLocation("Other Count Venue"), "Other Event", Category.COMEDY));

        reservationRepository.save(buildReservation(userOne, targetEvent, ReservationStatus.CONFIRMED, "39.99"));
        reservationRepository.save(buildReservation(userTwo, targetEvent, ReservationStatus.CONFIRMED, "44.99"));
        reservationRepository.save(buildReservation(userOne, otherEvent, ReservationStatus.CONFIRMED, "29.99"));

        long count = reservationRepository.countByEvent(targetEvent);

        assertEquals(2L, count);
    }

    private Location persistLocation(String name) {
        Location location = new Location(name, "456 Main St", "Montreal", "QC", "H2B2B2");
        entityManager.persist(location);
        entityManager.flush();
        return location;
    }

    private User buildUser(String email, String phoneNumber) {
        return new User("Test User", email, phoneNumber, "hashed-password", UserRole.CUSTOMER);
    }

    private Event buildEvent(Location location, String title, Category category) {
        OffsetDateTime start = OffsetDateTime.parse("2026-10-01T18:00:00Z");
        return new Event(
                location,
                title,
                start,
                start.plusHours(3),
                category,
                new BigDecimal("59.99"),
                EventStatus.SCHEDULED
        );
    }

    private Reservation buildReservation(User user, Event event, ReservationStatus status, String totalPrice) {
        return new Reservation(
                user,
                event,
                OffsetDateTime.parse("2026-05-01T12:00:00Z"),
                status,
                new BigDecimal(totalPrice)
        );
    }
}
