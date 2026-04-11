package com.pestcontrol.backend.integration.controller;

import com.pestcontrol.backend.api.AuthController;
import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.LoginResponse;
import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerIntegrationTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        AuthService authService = new AuthService(userRepository, passwordEncoder);
        authController = new AuthController(authService);
    }

    @Test
    void register_withValidEmailAndPassword_savesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        authController.registerUser(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withPhoneNumberOnly_savesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber("5141234567");
        request.setPassword("secret123");
        request.setFullName("Jane Doe");

        when(userRepository.existsByPhoneNumber("5141234567")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        authController.registerUser(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withDuplicateEmail_throws409AndDoesNotSave() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withDuplicatePhone_throws409AndDoesNotSave() {
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber("5141234567");
        request.setPassword("secret123");

        when(userRepository.existsByPhoneNumber("5141234567")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withNeitherEmailNorPhone_throws400() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("secret123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.registerUser(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withBlankPassword_throws400() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.registerUser(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withCorrectEmailCredentials_returns200WithToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret123");

        User user = buildUser(1L, "user@example.com", null, UserRole.CUSTOMER);
        user.setPasswordHash("hashed_password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
    }

    @Test
    void login_withCorrectPhoneCredentials_returns200WithToken() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("5141234567");
        request.setPassword("secret123");

        User user = buildUser(1L, null, "5141234567", UserRole.CUSTOMER);
        user.setPasswordHash("hashed_password");

        when(userRepository.findByPhoneNumber("5141234567")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getToken());
    }

    @Test
    void login_withWrongPassword_throws401() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongpassword");

        User user = buildUser(1L, "user@example.com", null, UserRole.CUSTOMER);
        user.setPasswordHash("hashed_password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_withUnknownEmail_throws401() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("secret123");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_withNeitherEmailNorPhone_throws400() {
        LoginRequest request = new LoginRequest();
        request.setPassword("secret123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.login(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private User buildUser(Long id, String email, String phoneNumber, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setUserRole(role);
        user.setPasswordHash("irrelevant_hash");
        return user;
    }
}