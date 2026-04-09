package com.pestcontrol.backend.integration.repository;

import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.service.EventService;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventRepositoryIntegrationTest {

    // --- Repository ---
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    // --- Service ---
    @Autowired
    private EventService eventService;

    @MockitoBean
    private JavaMailSender javaMailSender;

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

        List<EventResponse> results = eventService.getEvents(null, null, LocalDate.of(2026, 6, 30), null, null);

        assertEquals(1, results.size());
        assertEquals("Spring Fling", results.get(0).getTitle());
    }

    @Test
    void getEvents_withEndDateBeforeStartDate_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.getEvents(null,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 8, 1),
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

    private Event buildEventWithStatus(Location location, String title, Category category, OffsetDateTime startDateTime, EventStatus status) {
        return new Event(
                location,
                title,
                startDateTime,
                startDateTime.plusHours(2),
                category,
                new BigDecimal("49.99"),
                status
        );
    }
}
