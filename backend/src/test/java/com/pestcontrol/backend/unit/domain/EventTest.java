package com.pestcontrol.backend.unit.domain;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Domain Model Tests")
class EventTest {

    private Event event;
    private Location location;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setLocationId(1L);
        location.setAddressLine("123 Main Street");

        startDateTime = OffsetDateTime.of(2026, 4, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        endDateTime = OffsetDateTime.of(2026, 4, 15, 14, 0, 0, 0, ZoneOffset.UTC);

        event = new Event(location, "Pest Control Workshop", startDateTime, endDateTime,
                Category.CONCERT, BigDecimal.valueOf(50.00), EventStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should create event with constructor parameters")
    void testEventConstructor() {
        // Assert
        assertNotNull(event);
        assertEquals(location, event.getLocation());
        assertEquals("Pest Control Workshop", event.getTitle());
        assertEquals(startDateTime, event.getStartDateTime());
        assertEquals(endDateTime, event.getEndDateTime());
        assertEquals(Category.CONCERT, event.getCategory());
        assertEquals(BigDecimal.valueOf(50.00), event.getBasePrice());
        assertEquals(EventStatus.SCHEDULED, event.getStatus());
    }

    @Test
    @DisplayName("Should set and get event ID")
    void testSetGetEventId() {
        // Act
        event.setEventId(100L);

        // Assert
        assertEquals(100L, event.getEventId());
    }

    @Test
    @DisplayName("Should set and get location")
    void testSetGetLocation() {
        // Arrange
        Location newLocation = new Location();
        newLocation.setLocationId(2L);

        // Act
        event.setLocation(newLocation);

        // Assert
        assertEquals(newLocation, event.getLocation());
    }

    @Test
    @DisplayName("Should set and get title")
    void testSetGetTitle() {
        // Act
        event.setTitle("Termite Inspection Session");

        // Assert
        assertEquals("Termite Inspection Session", event.getTitle());
    }

    @Test
    @DisplayName("Should set and get start date time")
    void testSetGetStartDateTime() {
        // Arrange
        OffsetDateTime newStartDateTime = OffsetDateTime.of(2026, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        // Act
        event.setStartDateTime(newStartDateTime);

        // Assert
        assertEquals(newStartDateTime, event.getStartDateTime());
    }

    @Test
    @DisplayName("Should set and get end date time")
    void testSetGetEndDateTime() {
        // Arrange
        OffsetDateTime newEndDateTime = OffsetDateTime.of(2026, 5, 1, 15, 0, 0, 0, ZoneOffset.UTC);

        // Act
        event.setEndDateTime(newEndDateTime);

        // Assert
        assertEquals(newEndDateTime, event.getEndDateTime());
    }

    @Test
    @DisplayName("Should set and get category")
    void testSetGetCategory() {
        // Act
        event.setCategory(Category.FAMILY);

        // Assert
        assertEquals(Category.FAMILY, event.getCategory());
    }

    @Test
    @DisplayName("Should set and get base price")
    void testSetGetBasePrice() {
        // Act
        event.setBasePrice(BigDecimal.valueOf(75.99));

        // Assert
        assertEquals(BigDecimal.valueOf(75.99), event.getBasePrice());
    }

    @Test
    @DisplayName("Should set and get status")
    void testSetGetStatus() {
        // Act
        event.setStatus(EventStatus.CANCELLED);

        // Assert
        assertEquals(EventStatus.CANCELLED, event.getStatus());
    }

    @Test
    @DisplayName("Should initialize reservations as empty list")
    void testReservationsInitializedAsEmptyList() {
        // Assert
        assertNotNull(event.getReservations());
        assertTrue(event.getReservations().isEmpty());
    }

    @Test
    @DisplayName("Should get reservations list")
    void testGetReservations() {
        // Arrange & Act
        var reservations = event.getReservations();

        // Assert
        assertNotNull(reservations);
        assertEquals(0, reservations.size());
    }

    @Test
    @DisplayName("Should create default event with no-arg constructor")
    void testDefaultConstructor() {
        // Act
        Event newEvent = new Event();

        // Assert
        assertNotNull(newEvent);
        assertNull(newEvent.getEventId());
        assertNull(newEvent.getTitle());
    }

    @Test
    @DisplayName("Should support multiple event statuses")
    void testEventStatuses() {
        // Act & Assert
        event.setStatus(EventStatus.SCHEDULED);
        assertEquals(EventStatus.SCHEDULED, event.getStatus());

        event.setStatus(EventStatus.PAST);
        assertEquals(EventStatus.PAST, event.getStatus());

        event.setStatus(EventStatus.CANCELLED);
        assertEquals(EventStatus.CANCELLED, event.getStatus());
    }

    @Test
    @DisplayName("Should support multiple categories")
    void testEventCategories() {
        // Act & Assert
        event.setCategory(Category.CONCERT);
        assertEquals(Category.CONCERT, event.getCategory());

        event.setCategory(Category.SPORTS);
        assertEquals(Category.SPORTS, event.getCategory());

        event.setCategory(Category.ART_THEATER);
        assertEquals(Category.ART_THEATER, event.getCategory());
    }

    @Test
    @DisplayName("Should handle large base prices")
    void testLargeBasePrice() {
        // Act
        event.setBasePrice(BigDecimal.valueOf(9999.99));

        // Assert
        assertEquals(BigDecimal.valueOf(9999.99), event.getBasePrice());
    }

    @Test
    @DisplayName("Should handle zero base price")
    void testZeroBasePrice() {
        // Act
        event.setBasePrice(BigDecimal.ZERO);

        // Assert
        assertEquals(BigDecimal.ZERO, event.getBasePrice());
    }
}

