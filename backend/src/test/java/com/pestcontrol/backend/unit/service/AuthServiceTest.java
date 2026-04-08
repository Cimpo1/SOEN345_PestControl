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
        validRegisterRequest.setFullName("John Doe");
        validRegisterRequest.setEmail("john@example.com");
        validRegisterRequest.setPhoneNumber("1234567890");
        validRegisterRequest.setPassword("securePassword123");

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
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encodedPassword");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(validRegisterRequest.getPassword());
    }

    @Test
    @DisplayName("Should successfully register user with only email")
    void testRegisterSuccessWithEmailOnly() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPhoneNumber(null);
        request.setPassword("securePassword123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

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
        request.setFullName("Bob Smith");
        request.setEmail(null);
        request.setPhoneNumber("9876543210");
        request.setPassword("securePassword123");

        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        // Act
        authService.register(request);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully register user with blank email and phone only")
    void testRegisterSuccessWithBlankEmailAndPhoneOnly() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Phone User");
        request.setEmail("   ");
        request.setPhoneNumber("2223334444");
        request.setPassword("securePassword123");

        when(userRepository.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        authService.register(request);

        verify(userRepository, times(1)).save(argThat(user -> user.getEmail() == null &&
                "2223334444".equals(user.getPhoneNumber())));
    }

    @Test
    @DisplayName("Should successfully register user with email only and blank phone")
    void testRegisterSuccessWithEmailOnlyAndBlankPhone() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Email User");
        request.setEmail("email-only@example.com");
        request.setPhoneNumber("   ");
        request.setPassword("securePassword123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        authService.register(request);

        verify(userRepository, times(1)).save(argThat(user -> "email-only@example.com".equals(user.getEmail()) &&
                user.getPhoneNumber() == null));
    }

    @Test
    @DisplayName("Should throw exception when both email and phone are null")
    void testRegisterFailsWithoutEmailOrPhone() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Invalid User");
        request.setEmail(null);
        request.setPhoneNumber(null);
        request.setPassword("password123");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Email or phone required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when password is null")
    void testRegisterFailsWithoutPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Invalid User");
        request.setEmail("missing-password@example.com");
        request.setPhoneNumber(null);
        request.setPassword(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Password required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when password is blank")
    void testRegisterFailsWithBlankPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Invalid User");
        request.setEmail("blank-password@example.com");
        request.setPhoneNumber(null);
        request.setPassword("   ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Password required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegisterFailsWithDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(validRegisterRequest));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when phone number already exists")
    void testRegisterFailsWithDuplicatePhoneNumber() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.getPhoneNumber())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(validRegisterRequest));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Phone already exists", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password correctly before saving")
    void testPasswordEncodingDuringRegistration() {
        // Arrange
        String rawPassword = "rawPassword123";
        String encodedPassword = "encodedPasswordHash";
        validRegisterRequest.setPassword(rawPassword);

        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.getPhoneNumber())).thenReturn(false);
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
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encodedPassword");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository).save(argThat(user -> user.getUserRole() == UserRole.CUSTOMER));
    }

    @Test
    @DisplayName("Should save user with correct details")
    void testUserDetailsCorrectlySaved() {
        // Arrange
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(validRegisterRequest.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("encodedPassword");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository).save(argThat(user -> user.getFullName().equals(validRegisterRequest.getFullName()) &&
                user.getEmail().equals(validRegisterRequest.getEmail()) &&
                user.getPhoneNumber().equals(validRegisterRequest.getPhoneNumber())));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when both email and phone are null")
    void testLoginFailsWithoutEmailOrPhone() {
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPhoneNumber(null);
        request.setPassword("anyPassword");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Missing credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when both email and phone are blank")
    void testLoginFailsWithBlankEmailAndPhone() {
        LoginRequest request = new LoginRequest();
        request.setEmail("   ");
        request.setPhoneNumber("   ");
        request.setPassword("anyPassword");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Missing credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when email not found")
    void testLoginFailsWithInvalidEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when phone not found")
    void testLoginFailsWithInvalidPhone() {
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPhoneNumber("0000000000");
        request.setPassword("password");

        when(userRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when password does not match")
    void testLoginFailsWithIncorrectPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail(mockUser.getEmail());
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPasswordHash())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    @DisplayName("Should login successfully with email")
    void testLoginSuccessWithEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail(mockUser.getEmail());
        request.setPassword("correctPassword");

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPasswordHash())).thenReturn(true);

        try (MockedStatic<JWTService> jwtMock = mockStatic(JWTService.class)) {
            jwtMock.when(() -> JWTService.generateToken(mockUser)).thenReturn("mockToken");

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("mockToken", response.getToken());
            assertEquals(mockUser.getFullName(), response.getUser().getFullName());
        }
    }

    @Test
    @DisplayName("Should login successfully with phone")
    void testLoginSuccessWithPhone() {
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPhoneNumber(mockUser.getPhoneNumber());
        request.setPassword("correctPassword");

        when(userRepository.findByPhoneNumber(mockUser.getPhoneNumber())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.getPassword(), mockUser.getPasswordHash())).thenReturn(true);

        try (MockedStatic<JWTService> jwtMock = mockStatic(JWTService.class)) {
            jwtMock.when(() -> JWTService.generateToken(mockUser)).thenReturn("mockToken");

            LoginResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("mockToken", response.getToken());
            assertEquals(mockUser.getFullName(), response.getUser().getFullName());
        }
    }
}

