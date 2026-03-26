package com.pestcontrol.backend.domain;

import com.pestcontrol.backend.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Domain Model Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("John Doe", "john@example.com", "1234567890", "passwordHash", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Should create user with constructor parameters")
    void testUserConstructor() {
        // Assert
        assertNotNull(user);
        assertEquals("John Doe", user.getFullName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("1234567890", user.getPhoneNumber());
        assertEquals("passwordHash", user.getPasswordHash());
        assertEquals(UserRole.CUSTOMER, user.getUserRole());
    }

    @Test
    @DisplayName("Should set and get userId")
    void testSetGetUserId() {
        // Act
        user.setUserId(1L);

        // Assert
        assertEquals(1L, user.getUserId());
    }

    @Test
    @DisplayName("Should set and get full name")
    void testSetGetFullName() {
        // Act
        user.setFullName("Jane Doe");

        // Assert
        assertEquals("Jane Doe", user.getFullName());
    }

    @Test
    @DisplayName("Should set and get email")
    void testSetGetEmail() {
        // Act
        user.setEmail("jane@example.com");

        // Assert
        assertEquals("jane@example.com", user.getEmail());
    }

    @Test
    @DisplayName("Should set and get phone number")
    void testSetGetPhoneNumber() {
        // Act
        user.setPhoneNumber("9876543210");

        // Assert
        assertEquals("9876543210", user.getPhoneNumber());
    }

    @Test
    @DisplayName("Should set and get password hash")
    void testSetGetPasswordHash() {
        // Act
        user.setPasswordHash("newPasswordHash");

        // Assert
        assertEquals("newPasswordHash", user.getPasswordHash());
    }

    @Test
    @DisplayName("Should set and get user role")
    void testSetGetUserRole() {
        // Act
        user.setUserRole(UserRole.ADMIN);

        // Assert
        assertEquals(UserRole.ADMIN, user.getUserRole());
    }

    @Test
    @DisplayName("Should initialize reservations as empty list")
    void testReservationsInitializedAsEmptyList() {
        // Assert
        assertNotNull(user.getReservations());
        assertTrue(user.getReservations().isEmpty());
    }

    @Test
    @DisplayName("Should get reservations list")
    void testGetReservations() {
        // Arrange
        List<Reservation> reservations = user.getReservations();

        // Assert
        assertNotNull(reservations);
        assertEquals(0, reservations.size());
    }

    @Test
    @DisplayName("Should create default user with no-arg constructor")
    void testDefaultConstructor() {
        // Act
        User newUser = new User();

        // Assert
        assertNotNull(newUser);
        assertNull(newUser.getUserId());
        assertNull(newUser.getFullName());
        assertNull(newUser.getEmail());
    }

    @Test
    @DisplayName("Should support multiple role types")
    void testUserRoles() {
        // Act & Assert
        user.setUserRole(UserRole.CUSTOMER);
        assertEquals(UserRole.CUSTOMER, user.getUserRole());

        user.setUserRole(UserRole.ADMIN);
        assertEquals(UserRole.ADMIN, user.getUserRole());
    }

    @Test
    @DisplayName("Should handle null email")
    void testNullEmail() {
        // Act
        user.setEmail(null);

        // Assert
        assertNull(user.getEmail());
    }

    @Test
    @DisplayName("Should handle null phone number")
    void testNullPhoneNumber() {
        // Act
        user.setPhoneNumber(null);

        // Assert
        assertNull(user.getPhoneNumber());
    }
}

