package com.pestcontrol.backend.unit.domain;

import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ticket Domain Model Tests")
class TicketTest {

    private Ticket ticket;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        reservation.setReservationId(1L);

        ticket = new Ticket(reservation, BigDecimal.valueOf(50.00));
    }

    @Test
    @DisplayName("Should create ticket with constructor parameters")
    void testTicketConstructor() {
        // Assert
        assertNotNull(ticket);
        assertEquals(reservation, ticket.getReservation());
        assertEquals(BigDecimal.valueOf(50.00), ticket.getPrice());
    }

    @Test
    @DisplayName("Should set and get ticket ID")
    void testSetGetTicketId() {
        // Act
        ticket.setTicketId(1L);

        // Assert
        assertEquals(1L, ticket.getTicketId());
    }

    @Test
    @DisplayName("Should set and get reservation")
    void testSetGetReservation() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setReservationId(2L);

        // Act
        ticket.setReservation(newReservation);

        // Assert
        assertEquals(newReservation, ticket.getReservation());
        assertEquals(2L, ticket.getReservation().getReservationId());
    }

    @Test
    @DisplayName("Should set and get price")
    void testSetGetPrice() {
        // Act
        ticket.setPrice(BigDecimal.valueOf(75.99));

        // Assert
        assertEquals(BigDecimal.valueOf(75.99), ticket.getPrice());
    }

    @Test
    @DisplayName("Should create ticket with default constructor")
    void testDefaultConstructor() {
        // Arrange & Act
        Ticket newTicket = new Ticket();

        // Assert
        assertNotNull(newTicket);
        assertNull(newTicket.getTicketId());
        assertNull(newTicket.getReservation());
    }

    @Test
    @DisplayName("Should handle zero price")
    void testZeroPrice() {
        // Act
        ticket.setPrice(BigDecimal.ZERO);

        // Assert
        assertEquals(BigDecimal.ZERO, ticket.getPrice());
    }

    @Test
    @DisplayName("Should handle large prices")
    void testLargePrice() {
        // Act
        ticket.setPrice(BigDecimal.valueOf(9999.99));

        // Assert
        assertEquals(BigDecimal.valueOf(9999.99), ticket.getPrice());
    }

    @Test
    @DisplayName("Should handle decimal prices")
    void testDecimalPrice() {
        // Act
        ticket.setPrice(BigDecimal.valueOf(45.50));

        // Assert
        assertEquals(BigDecimal.valueOf(45.50), ticket.getPrice());
    }

    @Test
    @DisplayName("Should handle null reservation")
    void testNullReservation() {
        // Act
        ticket.setReservation(null);

        // Assert
        assertNull(ticket.getReservation());
    }

    @Test
    @DisplayName("Should maintain reservation reference integrity")
    void testReservationReferenceIntegrity() {
        // Arrange
        Reservation expectedReservation = new Reservation();
        expectedReservation.setReservationId(5L);

        // Act
        ticket.setReservation(expectedReservation);
        Reservation retrievedReservation = ticket.getReservation();

        // Assert
        assertSame(expectedReservation, retrievedReservation);
        assertEquals(5L, retrievedReservation.getReservationId());
    }

    @Test
    @DisplayName("Should allow multiple tickets with different prices")
    void testMultipleTickets() {
        // Arrange
        Ticket ticket1 = new Ticket();
        Ticket ticket2 = new Ticket();

        // Act
        ticket1.setTicketId(1L);
        ticket1.setPrice(BigDecimal.valueOf(50.00));
        ticket2.setTicketId(2L);
        ticket2.setPrice(BigDecimal.valueOf(75.00));

        // Assert
        assertEquals(1L, ticket1.getTicketId());
        assertEquals(2L, ticket2.getTicketId());
        assertEquals(BigDecimal.valueOf(50.00), ticket1.getPrice());
        assertEquals(BigDecimal.valueOf(75.00), ticket2.getPrice());
    }

    @Test
    @DisplayName("Should allow price changes")
    void testPriceChanges() {
        // Act
        ticket.setPrice(BigDecimal.valueOf(50.00));
        assertEquals(BigDecimal.valueOf(50.00), ticket.getPrice());

        ticket.setPrice(BigDecimal.valueOf(100.00));
        assertEquals(BigDecimal.valueOf(100.00), ticket.getPrice());

        // Assert
        assertEquals(BigDecimal.valueOf(100.00), ticket.getPrice());
    }
}

