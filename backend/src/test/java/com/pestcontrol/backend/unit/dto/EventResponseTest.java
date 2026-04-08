package com.pestcontrol.backend.unit.dto;

import com.pestcontrol.backend.api.dto.EventLocationResponse;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventResponseTest {

    @Test
    void testConstructorMapsEventCorrectly() {
        Event event = mock(Event.class);
        Location location = mock(Location.class);
        OffsetDateTime startDateTime = OffsetDateTime.parse("2026-04-15T10:00:00Z");
        OffsetDateTime endDateTime = OffsetDateTime.parse("2026-04-15T14:00:00Z");

        when(event.getEventId()).thenReturn(5L);
        when(event.getTitle()).thenReturn("Spring Pest Workshop");
        when(event.getStartDateTime()).thenReturn(startDateTime);
        when(event.getEndDateTime()).thenReturn(endDateTime);
        when(event.getCategory()).thenReturn(Category.CONCERT);
        when(event.getBasePrice()).thenReturn(BigDecimal.valueOf(49.99));
        when(event.getStatus()).thenReturn(EventStatus.SCHEDULED);
        when(event.getLocation()).thenReturn(location);
        when(location.getLocationId()).thenReturn(100L);
        when(location.getName()).thenReturn("Downtown Center");
        when(location.getAddressLine()).thenReturn("789 King Street");
        when(location.getCity()).thenReturn("Montreal");
        when(location.getProvince()).thenReturn("QC");
        when(location.getPostalCode()).thenReturn("H2H 2H2");

        EventResponse response = new EventResponse(event);

        assertAll(
                () -> assertEquals(5L, response.getEventId()),
                () -> assertEquals("Spring Pest Workshop", response.getTitle()),
                () -> assertEquals(startDateTime, response.getStartDateTime()),
                () -> assertEquals(endDateTime, response.getEndDateTime()),
                () -> assertEquals(Category.CONCERT, response.getCategory()),
                () -> assertEquals(BigDecimal.valueOf(49.99), response.getBasePrice()),
                () -> assertEquals(EventStatus.SCHEDULED, response.getStatus()),
                () -> assertEquals(100L, response.getLocation().getLocationId()),
                () -> assertEquals("Downtown Center", response.getLocation().getName()));
    }

    @Test
    void testSettersAndGetters() {
        Event event = mock(Event.class);
        when(event.getLocation()).thenReturn(mock(Location.class));

        EventResponse response = new EventResponse(event);
        EventLocationResponse locationResponse = new EventLocationResponse(mock(Location.class));
        OffsetDateTime startDateTime = OffsetDateTime.parse("2026-05-01T09:30:00Z");
        OffsetDateTime endDateTime = OffsetDateTime.parse("2026-05-01T11:30:00Z");

        response.setEventId(8L);
        response.setTitle("Inspection Session");
        response.setStartDateTime(startDateTime);
        response.setEndDateTime(endDateTime);
        response.setCategory(Category.SPORTS);
        response.setBasePrice(BigDecimal.valueOf(75.00));
        response.setStatus(EventStatus.PAST);
        response.setLocation(locationResponse);

        assertAll(
                () -> assertEquals(8L, response.getEventId()),
                () -> assertEquals("Inspection Session", response.getTitle()),
                () -> assertEquals(startDateTime, response.getStartDateTime()),
                () -> assertEquals(endDateTime, response.getEndDateTime()),
                () -> assertEquals(Category.SPORTS, response.getCategory()),
                () -> assertEquals(BigDecimal.valueOf(75.00), response.getBasePrice()),
                () -> assertEquals(EventStatus.PAST, response.getStatus()),
                () -> assertEquals(locationResponse, response.getLocation()));
    }
}