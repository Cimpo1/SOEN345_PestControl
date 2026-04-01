package com.pestcontrol.backend.unit.service;

import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.service.JWTService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWTService Tests")
class JWTServiceTest {

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(42L);
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPhoneNumber("1234567890");
        mockUser.setUserRole(UserRole.CUSTOMER);
        mockUser.setPasswordHash("someHash");
    }

    @Test
    @DisplayName("generateToken should return a non-null, non-empty token")
    void testGenerateTokenReturnsToken() {
        String token = JWTService.generateToken(mockUser);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("generateToken should return a valid JWT with three parts")
    void testGenerateTokenHasThreeParts() {
        String token = JWTService.generateToken(mockUser);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("validateToken should return true for a freshly generated token")
    void testValidateTokenReturnsTrueForValidToken() {
        String token = JWTService.generateToken(mockUser);
        assertTrue(JWTService.validateToken(token));
    }

    @Test
    @DisplayName("validateToken should return false for a tampered token")
    void testValidateTokenReturnsFalseForTamperedToken() {
        String token = JWTService.generateToken(mockUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(JWTService.validateToken(tampered));
    }

    @Test
    @DisplayName("validateToken should return false for a random string")
    void testValidateTokenReturnsFalseForGarbage() {
        assertFalse(JWTService.validateToken("not.a.jwt"));
    }

    @Test
    @DisplayName("validateToken should return false for an empty string")
    void testValidateTokenReturnsFalseForEmptyString() {
        assertFalse(JWTService.validateToken(""));
    }

    @Test
    @DisplayName("getClaims should return non-null Claims for a valid token")
    void testGetClaimsReturnsClaimsForValidToken() {
        String token = JWTService.generateToken(mockUser);
        Claims claims = JWTService.getClaims(token);
        assertNotNull(claims);
    }

    @Test
    @DisplayName("getClaims should throw RuntimeException for an invalid token")
    void testGetClaimsThrowsForInvalidToken() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> JWTService.getClaims("invalid.token.here"));
        assertEquals("Invalid JWT token", ex.getMessage());
    }

    @Test
    @DisplayName("getUserId should return the correct user ID")
    void testGetUserIdReturnsCorrectId() {
        String token = JWTService.generateToken(mockUser);
        Long userId = JWTService.getUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("getRole should return the correct role")
    void testGetRoleReturnsCorrectRole() {
        String token = JWTService.generateToken(mockUser);
        String role = JWTService.getRole(token);
        assertEquals(UserRole.CUSTOMER.name(), role);
    }

    @Test
    @DisplayName("getFullName should return the correct full name")
    void testGetFullNameReturnsCorrectName() {
        String token = JWTService.generateToken(mockUser);
        assertEquals("John Doe", JWTService.getFullName(token));
    }


    @Test
    @DisplayName("getEmail should return the correct email")
    void testGetEmailReturnsCorrectEmail() {
        String token = JWTService.generateToken(mockUser);
        assertEquals("john@example.com", JWTService.getEmail(token));
    }


    @Test
    @DisplayName("getPhoneNumber should return the correct phone number")
    void testGetPhoneNumberReturnsCorrectPhoneNumber() {
        String token = JWTService.generateToken(mockUser);
        assertEquals("1234567890", JWTService.getPhoneNumber(token));
    }

    @Test
    @DisplayName("getPhoneNumber should return null when phone number is null")
    void testGetPhoneNumberReturnsNullWhenNotSet() {
        mockUser.setPhoneNumber(null);
        String token = JWTService.generateToken(mockUser);
        assertNull(JWTService.getPhoneNumber(token));
    }

    @Test
    @DisplayName("getEmail should return null when email is null")
    void testGetEmailReturnsNullWhenNotSet() {
        mockUser.setEmail(null);
        String token = JWTService.generateToken(mockUser);
        assertNull(JWTService.getEmail(token));
    }
}
