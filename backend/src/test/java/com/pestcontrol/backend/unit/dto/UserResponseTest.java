package com.pestcontrol.backend.unit.dto;

import com.pestcontrol.backend.api.dto.UserResponse;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserResponseTest {

    @Test
    void testConstructorMapsUserCorrectly() {
        // Arrange
        User user = mock(User.class);

        Long userId = 1L;
        String fullName = "John Doe";
        String email = "john@example.com";
        String phone = "+123456789";
        UserRole role = UserRole.ADMIN;

        when(user.getUserId()).thenReturn(userId);
        when(user.getFullName()).thenReturn(fullName);
        when(user.getEmail()).thenReturn(email);
        when(user.getPhoneNumber()).thenReturn(phone);
        when(user.getUserRole()).thenReturn(role);

        // Act
        UserResponse response = new UserResponse(user);

        // Assert
        assertAll(
                () -> assertEquals(userId, response.getUserId()),
                () -> assertEquals(fullName, response.getFullName()),
                () -> assertEquals(email, response.getEmail()),
                () -> assertEquals(phone, response.getPhoneNumber()),
                () -> assertEquals(role, response.getUserRole())
        );
    }

    @Test
    void testSettersAndGetters() {
        UserResponse response = new UserResponse(mock(User.class));

        Long userId = 2L;
        String fullName = "Jane Doe";
        String email = "jane@example.com";
        String phone = "+987654321";
        UserRole role = UserRole.CUSTOMER;

        response.setUserId(userId);
        response.setFullName(fullName);
        response.setEmail(email);
        response.setPhoneNumber(phone);
        response.setUserRole(role);

        assertAll(
                () -> assertEquals(userId, response.getUserId()),
                () -> assertEquals(fullName, response.getFullName()),
                () -> assertEquals(email, response.getEmail()),
                () -> assertEquals(phone, response.getPhoneNumber()),
                () -> assertEquals(role, response.getUserRole())
        );
    }
}
