package com.pestcontrol.backend.unit.service;

import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.service.EventService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event montrealConcert;
    private Event lavalSports;
    private Event cancelledEvent;

    @BeforeEach
    void setUp() {
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
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventService.getEvents(
                        null,
                        LocalDate.parse("2026-08-01"),
                        LocalDate.parse("2026-07-01"),
                        null,
                        List.of()));

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
}