package com.pestcontrol.backend.unit.dto;

import com.pestcontrol.backend.api.dto.EventLocationResponse;
import com.pestcontrol.backend.domain.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventLocationResponseTest {

    @Test
    void testConstructorMapsLocationCorrectly() {
        Location location = mock(Location.class);

        when(location.getLocationId()).thenReturn(10L);
        when(location.getName()).thenReturn("North Branch");
        when(location.getAddressLine()).thenReturn("123 Main Street");
        when(location.getCity()).thenReturn("Montreal");
        when(location.getProvince()).thenReturn("QC");
        when(location.getPostalCode()).thenReturn("H1H 1H1");

        EventLocationResponse response = new EventLocationResponse(location);

        assertAll(
                () -> assertEquals(10L, response.getLocationId()),
                () -> assertEquals("North Branch", response.getName()),
                () -> assertEquals("123 Main Street", response.getAddressLine()),
                () -> assertEquals("Montreal", response.getCity()),
                () -> assertEquals("QC", response.getProvince()),
                () -> assertEquals("H1H 1H1", response.getPostalCode()));
    }

    @Test
    void testSettersAndGetters() {
        EventLocationResponse response = new EventLocationResponse(mock(Location.class));

        response.setLocationId(20L);
        response.setName("South Branch");
        response.setAddressLine("456 Oak Avenue");
        response.setCity("Quebec City");
        response.setProvince("QC");
        response.setPostalCode("G1A 1A1");

        assertAll(
                () -> assertEquals(20L, response.getLocationId()),
                () -> assertEquals("South Branch", response.getName()),
                () -> assertEquals("456 Oak Avenue", response.getAddressLine()),
                () -> assertEquals("Quebec City", response.getCity()),
                () -> assertEquals("QC", response.getProvince()),
                () -> assertEquals("G1A 1A1", response.getPostalCode()));
    }
}