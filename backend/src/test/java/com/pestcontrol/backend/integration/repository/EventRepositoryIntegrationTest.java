package com.pestcontrol.backend.integration.repository;

import com.pestcontrol.backend.domain.*;
import com.pestcontrol.backend.domain.enums.*;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.service.ReservationEmailService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventRepositoryIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

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
    void findByCategory_whenEventsExist_returnsOnlyMatchingEvents() {
        Location location = persistLocation("Olympic Stadium");
        eventRepository.save(buildEvent(location, "Rock Show", Category.CONCERT, OffsetDateTime.parse("2026-07-01T19:00:00Z")));
        eventRepository.save(buildEvent(location, "Soccer Match", Category.SPORTS, OffsetDateTime.parse("2026-07-02T19:00:00Z")));

        var events = eventRepository.findByCategory(Category.CONCERT);

        assertEquals(1, events.size());
        assertEquals("Rock Show", events.get(0).getTitle());
    }

    @Test
    void findByLocation_whenEventsExist_returnsOnlyMatchingEvents() {
        Location montreal = persistLocation("Bell Centre");
        Location laval = persistLocation("Place Bell");
        eventRepository.save(buildEvent(montreal, "Comedy Night", Category.COMEDY, OffsetDateTime.parse("2026-08-01T20:00:00Z")));
        eventRepository.save(buildEvent(laval, "Family Fun", Category.FAMILY, OffsetDateTime.parse("2026-08-02T12:00:00Z")));

        var events = eventRepository.findByLocation(montreal);

        assertEquals(1, events.size());
        assertEquals("Comedy Night", events.get(0).getTitle());
    }

    @Test
    void findByStartDateTimeAfter_whenMixedDates_returnsOnlyLaterEvents() {
        Location location = persistLocation("MTL Arena");
        eventRepository.save(buildEvent(location, "Past Event", Category.ART_THEATER, OffsetDateTime.parse("2026-01-01T10:00:00Z")));
        eventRepository.save(buildEvent(location, "Future Event", Category.ART_THEATER, OffsetDateTime.parse("2026-12-01T10:00:00Z")));

        var events = eventRepository.findByStartDateTimeAfter(OffsetDateTime.parse("2026-06-01T00:00:00Z"));

        assertEquals(1, events.size());
        assertEquals("Future Event", events.get(0).getTitle());
    }

    @Test
    void findByCategoryAndLocation_whenEventsExist_returnsOnlyMatchingEvents() {
        Location locationOne = persistLocation("Venue One");
        Location locationTwo = persistLocation("Venue Two");
        eventRepository.save(buildEvent(locationOne, "Concert One", Category.CONCERT, OffsetDateTime.parse("2026-09-10T18:00:00Z")));
        eventRepository.save(buildEvent(locationTwo, "Concert Two", Category.CONCERT, OffsetDateTime.parse("2026-09-11T18:00:00Z")));
        eventRepository.save(buildEvent(locationOne, "Sport One", Category.SPORTS, OffsetDateTime.parse("2026-09-12T18:00:00Z")));

        var events = eventRepository.findByCategoryAndLocation(Category.CONCERT, locationOne);

        assertEquals(1, events.size());
        assertEquals("Concert One", events.get(0).getTitle());
    }

    @Test
    void findByTitleContainingIgnoreCase_whenTitleMatches_returnsMatchingEvents() {
        Location location = persistLocation("Case Venue");
        eventRepository.save(buildEvent(location, "Summer Jazz Festival", Category.CONCERT, OffsetDateTime.parse("2026-07-15T18:00:00Z")));
        eventRepository.save(buildEvent(location, "Winter Market", Category.FAMILY, OffsetDateTime.parse("2026-12-10T11:00:00Z")));

        var events = eventRepository.findByTitleContainingIgnoreCase("jAzZ");

        assertEquals(1, events.size());
        assertEquals("Summer Jazz Festival", events.get(0).getTitle());
    }

    private Location persistLocation(String name) {
        Location location = new Location(name, "123 Main St", "Montreal", "QC", "H1A1A1");
        entityManager.persist(location);
        entityManager.flush();
        return location;
    }

    private Event buildEvent(Location location, String title, Category category, OffsetDateTime startDateTime) {
        return new Event(
                location,
                title,
                startDateTime,
                startDateTime.plusHours(2),
                category,
                new BigDecimal("49.99"),
                EventStatus.SCHEDULED
        );
    }
}
