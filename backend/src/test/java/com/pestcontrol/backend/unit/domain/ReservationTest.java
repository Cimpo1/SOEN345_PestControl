package com.pestcontrol.backend.unit.domain;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reservation Domain Model Tests")
class ReservationTest {

    private Reservation reservation;
    private User user;
    private Event event;
    private OffsetDateTime creationDate;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setFullName("John Doe");

        event = new Event();
        event.setEventId(1L);
        event.setTitle("Workshop");

        creationDate = OffsetDateTime.of(2026, 3, 22, 10, 0, 0, 0, ZoneOffset.UTC);

        reservation = new Reservation(user, event, creationDate, ReservationStatus.CONFIRMED, BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("Should create reservation with constructor parameters")
    void testReservationConstructor() {
        // Assert
        assertNotNull(reservation);
        assertEquals(user, reservation.getUser());
        assertEquals(event, reservation.getEvent());
        assertEquals(creationDate, reservation.getCreationDate());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(BigDecimal.valueOf(100.00), reservation.getTotalPrice());
    }

    @Test
    @DisplayName("Should set and get reservation ID")
    void testSetGetReservationId() {
        // Act
        reservation.setReservationId(50L);

        // Assert
        assertEquals(50L, reservation.getReservationId());
    }

    @Test
    @DisplayName("Should set and get user")
    void testSetGetUser() {
        // Arrange
        User newUser = new User();
        newUser.setUserId(2L);
        newUser.setFullName("Jane Doe");

        // Act
        reservation.setUser(newUser);

        // Assert
        assertEquals(newUser, reservation.getUser());
        assertEquals(2L, reservation.getUser().getUserId());
    }

    @Test
    @DisplayName("Should set and get event")
    void testSetGetEvent() {
        // Arrange
        Event newEvent = new Event();
        newEvent.setEventId(2L);
        newEvent.setTitle("Training Session");

        // Act
        reservation.setEvent(newEvent);

        // Assert
        assertEquals(newEvent, reservation.getEvent());
        assertEquals(2L, reservation.getEvent().getEventId());
    }

    @Test
    @DisplayName("Should set and get creation date")
    void testSetGetCreationDate() {
        // Arrange
        OffsetDateTime newCreationDate = OffsetDateTime.of(2026, 3, 23, 14, 30, 0, 0, ZoneOffset.UTC);

        // Act
        reservation.setCreationDate(newCreationDate);

        // Assert
        assertEquals(newCreationDate, reservation.getCreationDate());
    }

    @Test
    @DisplayName("Should set and get status")
    void testSetGetStatus() {
        // Act
        reservation.setStatus(ReservationStatus.PENDING);

        // Assert
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
    }

    @Test
    @DisplayName("Should set and get total price")
    void testSetGetTotalPrice() {
        // Act
        reservation.setTotalPrice(BigDecimal.valueOf(250.00));

        // Assert
        assertEquals(BigDecimal.valueOf(250.00), reservation.getTotalPrice());
    }

    @Test
    @DisplayName("Should initialize tickets as empty list")
    void testTicketsInitializedAsEmptyList() {
        // Assert
        assertNotNull(reservation.getTickets());
        assertTrue(reservation.getTickets().isEmpty());
    }

    @Test
    @DisplayName("Should get tickets list")
    void testGetTickets() {
        // Arrange & Act
        var tickets = reservation.getTickets();

        // Assert
        assertNotNull(tickets);
        assertEquals(0, tickets.size());
    }

    @Test
    @DisplayName("Should create default reservation with no-arg constructor")
    void testDefaultConstructor() {
        // Act
        Reservation newReservation = new Reservation();

        // Assert
        assertNotNull(newReservation);
        assertNull(newReservation.getReservationId());
        assertNull(newReservation.getUser());
        assertNull(newReservation.getEvent());
    }

    @Test
    @DisplayName("Should support multiple reservation statuses")
    void testReservationStatuses() {
        // Act & Assert
        reservation.setStatus(ReservationStatus.PENDING);
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());

        reservation.setStatus(ReservationStatus.CONFIRMED);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());

        reservation.setStatus(ReservationStatus.CANCELLED);
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    @DisplayName("Should handle zero total price")
    void testZeroTotalPrice() {
        // Act
        reservation.setTotalPrice(BigDecimal.ZERO);

        // Assert
        assertEquals(BigDecimal.ZERO, reservation.getTotalPrice());
    }

    @Test
    @DisplayName("Should handle large total prices")
    void testLargeTotalPrice() {
        // Act
        reservation.setTotalPrice(BigDecimal.valueOf(99999.99));

        // Assert
        assertEquals(BigDecimal.valueOf(99999.99), reservation.getTotalPrice());
    }

    @Test
    @DisplayName("Should handle decimal total prices")
    void testDecimalTotalPrice() {
        // Act
        reservation.setTotalPrice(BigDecimal.valueOf(123.45));

        // Assert
        assertEquals(BigDecimal.valueOf(123.45), reservation.getTotalPrice());
    }

    @Test
    @DisplayName("Should maintain user reference")
    void testUserReferenceIntegrity() {
        // Arrange
        User expectedUser = new User();
        expectedUser.setUserId(5L);

        // Act
        reservation.setUser(expectedUser);
        User retrievedUser = reservation.getUser();

        // Assert
        assertSame(expectedUser, retrievedUser);
        assertEquals(5L, retrievedUser.getUserId());
    }

    @Test
    @DisplayName("Should maintain event reference")
    void testEventReferenceIntegrity() {
        // Arrange
        Event expectedEvent = new Event();
        expectedEvent.setEventId(10L);

        // Act
        reservation.setEvent(expectedEvent);
        Event retrievedEvent = reservation.getEvent();

        // Assert
        assertSame(expectedEvent, retrievedEvent);
        assertEquals(10L, retrievedEvent.getEventId());
    }
}

