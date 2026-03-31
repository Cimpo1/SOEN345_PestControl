package com.pestcontrol.backend.unit.service;

import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.LoginResponse;
import com.pestcontrol.backend.api.dto.UserResponse;
import com.pestcontrol.backend.service.JWTService;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
@Profile("test")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;

    private User mockUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.fullName = "John Doe";
        validRegisterRequest.email = "john@example.com";
        validRegisterRequest.phoneNumber = "1234567890";
        validRegisterRequest.password = "securePassword123";

        mockUser = new User();
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPhoneNumber("1234567890");
        mockUser.setPasswordHash("encodedPassword");
        mockUser.setUserRole(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Should successfully register user with email and phone")
    void testRegisterSuccessWithEmailAndPhone() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password)).thenReturn("encodedPassword");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(validRegisterRequest.password);
    }

    @Test
    @DisplayName("Should successfully register user with only email")
    void testRegisterSuccessWithEmailOnly() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.fullName = "Jane Doe";
        request.email = "jane@example.com";
        request.phoneNumber = null;
        request.password = "securePassword123";

        when(userRepository.existsByEmail(request.email)).thenReturn(false);
        when(passwordEncoder.encode(request.password)).thenReturn("encodedPassword");

        // Act
        authService.register(request);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully register user with only phone number")
    void testRegisterSuccessWithPhoneOnly() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.fullName = "Bob Smith";
        request.email = null;
        request.phoneNumber = "9876543210";
        request.password = "securePassword123";

        when(userRepository.existsByPhoneNumber(request.phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(request.password)).thenReturn("encodedPassword");

        // Act
        authService.register(request);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when both email and phone are null")
    void testRegisterFailsWithoutEmailOrPhone() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.fullName = "Invalid User";
        request.email = null;
        request.phoneNumber = null;
        request.password = "password123";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertEquals("Email or phone required", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegisterFailsWithDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(validRegisterRequest));
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when phone number already exists")
    void testRegisterFailsWithDuplicatePhoneNumber() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.phoneNumber)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(validRegisterRequest));
        assertEquals("Phone already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password correctly before saving")
    void testPasswordEncodingDuringRegistration() {
        // Arrange
        String rawPassword = "rawPassword123";
        String encodedPassword = "encodedPasswordHash";
        validRegisterRequest.password = rawPassword;

        when(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(user -> user.getPasswordHash().equals(encodedPassword)));
    }

    @Test
    @DisplayName("Should set user role to CUSTOMER on registration")
    void testUserRoleSetToCustomer() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password)).thenReturn("encodedPassword");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository).save(argThat(user -> user.getUserRole() == UserRole.CUSTOMER));
    }

    @Test
    @DisplayName("Should save user with correct details")
    void testUserDetailsCorrectlySaved() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password)).thenReturn("encodedPassword");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository).save(argThat(user ->
            user.getFullName().equals(validRegisterRequest.fullName) &&
            user.getEmail().equals(validRegisterRequest.email) &&
            user.getPhoneNumber().equals(validRegisterRequest.phoneNumber)
        ));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when both email and phone are null")
    void testLoginFailsWithoutEmailOrPhone() {
        LoginRequest request = new LoginRequest();
        request.email = null;
        request.phoneNumber = null;
        request.password = "anyPassword";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Missing credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when email not found")
    void testLoginFailsWithInvalidEmail() {
        LoginRequest request = new LoginRequest();
        request.email = "invalid@example.com";
        request.password = "password";

        when(userRepository.findByEmail(request.email)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when phone not found")
    void testLoginFailsWithInvalidPhone() {
        LoginRequest request = new LoginRequest();
        request.email = null;
        request.phoneNumber = "0000000000";
        request.password = "password";

        when(userRepository.findByPhoneNumber(request.phoneNumber)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when password does not match")
    void testLoginFailsWithIncorrectPassword() {
        LoginRequest request = new LoginRequest();
        request.email = mockUser.getEmail();
        request.password = "wrongPassword";

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password, mockUser.getPasswordHash())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should login successfully with email")
    void testLoginSuccessWithEmail() {
        LoginRequest request = new LoginRequest();
        request.email = mockUser.getEmail();
        request.password = "correctPassword";

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password, mockUser.getPasswordHash())).thenReturn(true);

        try (MockedStatic<JWTService> jwtMock = mockStatic(JWTService.class)) {
            jwtMock.when(() -> JWTService.generateToken(mockUser)).thenReturn("mockToken");

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("mockToken", response.token);
            assertEquals(mockUser.getFullName(), response.user.fullName);
        }
    }

    @Test
    @DisplayName("Should login successfully with phone")
    void testLoginSuccessWithPhone() {
        LoginRequest request = new LoginRequest();
        request.email = null;
        request.phoneNumber = mockUser.getPhoneNumber();
        request.password = "correctPassword";

        when(userRepository.findByPhoneNumber(mockUser.getPhoneNumber())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password, mockUser.getPasswordHash())).thenReturn(true);

        try (MockedStatic<JWTService> jwtMock = mockStatic(JWTService.class)) {
            jwtMock.when(() -> JWTService.generateToken(mockUser)).thenReturn("mockToken");

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("mockToken", response.token);
            assertEquals(mockUser.getFullName(), response.user.fullName);
        }
    }
}

