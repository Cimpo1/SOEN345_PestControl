package com.pestcontrol.backend.unit.dto;

import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.api.dto.ReservationResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationResponseTest {

    @Test
    void constructor_shouldMapReservationAndInteractionStatus() {
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);
        Location location = mock(Location.class);
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-01-15T10:00:00Z");

        when(reservation.getReservationId()).thenReturn(7L);
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getCreationDate()).thenReturn(creationDate);
        when(reservation.getEvent()).thenReturn(event);
        when(event.getLocation()).thenReturn(location);

        ReservationResponse response = new ReservationResponse(reservation, "REGISTERED");

        assertAll(
                () -> assertEquals(7L, response.getReservationId()),
                () -> assertEquals("CONFIRMED", response.getReservationStatus()),
                () -> assertEquals("REGISTERED", response.getInteractionStatus()),
                () -> assertEquals(creationDate, response.getCreationDate()));
    }

    @Test
    void setters_shouldUpdateAllFields() {
        Reservation reservation = mock(Reservation.class);
        Event event = mock(Event.class);
        Location location = mock(Location.class);
        when(reservation.getEvent()).thenReturn(event);
        when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING);
        when(event.getLocation()).thenReturn(location);

        ReservationResponse response = new ReservationResponse(reservation, "REGISTERED");
        EventResponse eventResponse = new EventResponse(event);
        OffsetDateTime creationDate = OffsetDateTime.parse("2026-06-20T12:30:00Z");

        response.setReservationId(15L);
        response.setReservationStatus("CANCELLED");
        response.setInteractionStatus("CANCELLED");
        response.setCreationDate(creationDate);
        response.setEvent(eventResponse);

        assertAll(
                () -> assertEquals(15L, response.getReservationId()),
                () -> assertEquals("CANCELLED", response.getReservationStatus()),
                () -> assertEquals("CANCELLED", response.getInteractionStatus()),
                () -> assertEquals(creationDate, response.getCreationDate()),
                () -> assertEquals(eventResponse, response.getEvent()));
    }
}
