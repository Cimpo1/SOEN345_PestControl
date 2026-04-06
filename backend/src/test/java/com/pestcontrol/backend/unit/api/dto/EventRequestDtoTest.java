package com.pestcontrol.backend.unit.api.dto;

import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventLocationRequest;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventRequestDtoTest {

    @Test
    void createEventRequest_shouldStoreAndReturnAllFields() {
        EventLocationRequest location = new EventLocationRequest();
        location.setName("Bell Centre");
        location.setAddressLine("1 Arena Way");
        location.setCity("Montreal");
        location.setProvince("QC");
        location.setPostalCode("H1A1A1");

        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Jazz Night");
        request.setStartDateTime(OffsetDateTime.parse("2026-07-10T19:00:00Z"));
        request.setEndDateTime(OffsetDateTime.parse("2026-07-10T22:00:00Z"));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("49.99"));
        request.setLocationId(12L);
        request.setLocation(location);

        assertEquals("Jazz Night", request.getTitle());
        assertEquals(OffsetDateTime.parse("2026-07-10T19:00:00Z"), request.getStartDateTime());
        assertEquals(OffsetDateTime.parse("2026-07-10T22:00:00Z"), request.getEndDateTime());
        assertEquals("CONCERT", request.getCategory());
        assertEquals(new BigDecimal("49.99"), request.getBasePrice());
        assertEquals(12L, request.getLocationId());
        assertNotNull(request.getLocation());
        assertEquals("Bell Centre", request.getLocation().getName());
    }

    @Test
    void eventLocationRequest_shouldStoreAndReturnAllFields() {
        EventLocationRequest location = new EventLocationRequest();
        location.setName("Place Bell");
        location.setAddressLine("2 Arena Way");
        location.setCity("Laval");
        location.setProvince("QC");
        location.setPostalCode("H2B2B2");

        assertEquals("Place Bell", location.getName());
        assertEquals("2 Arena Way", location.getAddressLine());
        assertEquals("Laval", location.getCity());
        assertEquals("QC", location.getProvince());
        assertEquals("H2B2B2", location.getPostalCode());
    }

    @Test
    void updateEventRequest_shouldStoreAndReturnAllFields() {
        EventLocationRequest location = new EventLocationRequest();
        location.setName("Bell Centre");
        location.setAddressLine("1 Arena Way");
        location.setCity("Montreal");
        location.setProvince("QC");
        location.setPostalCode("H1A1A1");

        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Updated Jazz Night");
        request.setStartDateTime(OffsetDateTime.parse("2026-07-10T20:00:00Z"));
        request.setEndDateTime(OffsetDateTime.parse("2026-07-10T23:00:00Z"));
        request.setCategory("CONCERT");
        request.setBasePrice(new BigDecimal("59.99"));
        request.setLocationId(12L);
        request.setLocation(location);

        assertEquals("Updated Jazz Night", request.getTitle());
        assertEquals(OffsetDateTime.parse("2026-07-10T20:00:00Z"), request.getStartDateTime());
        assertEquals(OffsetDateTime.parse("2026-07-10T23:00:00Z"), request.getEndDateTime());
        assertEquals("CONCERT", request.getCategory());
        assertEquals(new BigDecimal("59.99"), request.getBasePrice());
        assertEquals(12L, request.getLocationId());
        assertNotNull(request.getLocation());
        assertEquals("Bell Centre", request.getLocation().getName());
    }
}