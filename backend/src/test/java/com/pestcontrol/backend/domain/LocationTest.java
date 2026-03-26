package com.pestcontrol.backend.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Location Domain Model Tests")
class LocationTest {

    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location("Main Hall", "123 Main Street", "Montreal", "QC", "H1A 1A1");
    }

    @Test
    @DisplayName("Should create location with constructor parameters")
    void testLocationConstructor() {
        // Assert
        assertNotNull(location);
        assertEquals("Main Hall", location.getName());
        assertEquals("123 Main Street", location.getAddressLine());
        assertEquals("Montreal", location.getCity());
        assertEquals("QC", location.getProvince());
        assertEquals("H1A 1A1", location.getPostalCode());
    }

    @Test
    @DisplayName("Should set and get location ID")
    void testSetGetLocationId() {
        // Act
        location.setLocationId(1L);

        // Assert
        assertEquals(1L, location.getLocationId());
    }

    @Test
    @DisplayName("Should set and get name")
    void testSetGetName() {
        // Act
        location.setName("Downtown Hall");

        // Assert
        assertEquals("Downtown Hall", location.getName());
    }

    @Test
    @DisplayName("Should set and get address line")
    void testSetGetAddressLine() {
        // Act
        location.setAddressLine("456 Oak Avenue");

        // Assert
        assertEquals("456 Oak Avenue", location.getAddressLine());
    }

    @Test
    @DisplayName("Should set and get city")
    void testSetGetCity() {
        // Act
        location.setCity("Toronto");

        // Assert
        assertEquals("Toronto", location.getCity());
    }

    @Test
    @DisplayName("Should set and get province")
    void testSetGetProvince() {
        // Act
        location.setProvince("ON");

        // Assert
        assertEquals("ON", location.getProvince());
    }

    @Test
    @DisplayName("Should set and get postal code")
    void testSetGetPostalCode() {
        // Act
        location.setPostalCode("M5H 2N2");

        // Assert
        assertEquals("M5H 2N2", location.getPostalCode());
    }

    @Test
    @DisplayName("Should create location with default constructor")
    void testDefaultConstructor() {
        // Arrange & Act
        Location newLocation = new Location();

        // Assert
        assertNotNull(newLocation);
        assertNull(newLocation.getLocationId());
        assertNull(newLocation.getName());
    }

    @Test
    @DisplayName("Should handle null name")
    void testNullName() {
        // Act
        location.setName(null);

        // Assert
        assertNull(location.getName());
    }

    @Test
    @DisplayName("Should handle null address line")
    void testNullAddressLine() {
        // Act
        location.setAddressLine(null);

        // Assert
        assertNull(location.getAddressLine());
    }

    @Test
    @DisplayName("Should handle null city")
    void testNullCity() {
        // Act
        location.setCity(null);

        // Assert
        assertNull(location.getCity());
    }

    @Test
    @DisplayName("Should handle long address strings")
    void testLongAddressLine() {
        // Arrange
        String longAddress = "123 Very Long Street Name, Building A, Suite 500";

        // Act
        location.setAddressLine(longAddress);

        // Assert
        assertEquals(longAddress, location.getAddressLine());
    }

    @Test
    @DisplayName("Should handle various Canadian provinces")
    void testVariousProvinces() {
        // Test different provinces
        location.setProvince("AB");
        assertEquals("AB", location.getProvince());

        location.setProvince("BC");
        assertEquals("BC", location.getProvince());

        location.setProvince("ON");
        assertEquals("ON", location.getProvince());
    }

    @Test
    @DisplayName("Should handle various postal codes")
    void testVariousPostalCodes() {
        // Test different postal code formats
        location.setPostalCode("H1A 1A1");
        assertEquals("H1A 1A1", location.getPostalCode());

        location.setPostalCode("M5V 3A8");
        assertEquals("M5V 3A8", location.getPostalCode());
    }
}

