package com.pestcontrol.backend.integration.service;

import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.LoginResponse;
import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_withEmailOnly_persistsUserAsCustomer() {
        RegisterRequest request = buildRegisterRequest("auth.email@test.com", null, "SecurePass1!");
        authService.register(request);
        var saved = userRepository.findByEmail("auth.email@test.com");
        assertTrue(saved.isPresent());
        assertEquals("Auth User", saved.get().getFullName());
        assertEquals(UserRole.CUSTOMER, saved.get().getUserRole());
        assertNull(saved.get().getPhoneNumber());
        assertNotEquals("SecurePass1!", saved.get().getPasswordHash());
    }

    @Test
    void register_withPhoneOnly_persistsUserWithNullEmail() {
        RegisterRequest request = buildRegisterRequest(null, "5141110001", "SecurePass1!");
        authService.register(request);
        var saved = userRepository.findByPhoneNumber("5141110001");
        assertTrue(saved.isPresent());
        assertNull(saved.get().getEmail());
    }

    @Test
    void register_withBothEmailAndPhone_persistsAllFields() {
        RegisterRequest request = buildRegisterRequest("both@test.com", "5141110002", "SecurePass1!");
        authService.register(request);
        var saved = userRepository.findByEmail("both@test.com");
        assertTrue(saved.isPresent());
        assertEquals("5141110002", saved.get().getPhoneNumber());
    }

    @Test
    void register_normalizesEmailToLowercase() {
        RegisterRequest request = buildRegisterRequest("Upper@Test.COM", null, "SecurePass1!");
        authService.register(request);
        assertTrue(userRepository.findByEmail("upper@test.com").isPresent());
        assertTrue(userRepository.findByEmail("Upper@Test.COM").isEmpty());
    }

    @Test
    void register_whenEmailAlreadyExists_throwsConflict() {
        userRepository.save(buildUser("dup@test.com", null));
        RegisterRequest request = buildRegisterRequest("dup@test.com", null, "SecurePass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_whenPhoneAlreadyExists_throwsConflict() {
        userRepository.save(buildUser(null, "5141110003"));
        RegisterRequest request = buildRegisterRequest(null, "5141110003", "SecurePass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void register_withNeitherEmailNorPhone_throwsBadRequest() {
        RegisterRequest request = buildRegisterRequest(null, null, "SecurePass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void register_withBlankEmailAndPhone_throwsBadRequest() {
        RegisterRequest request = buildRegisterRequest("   ", "   ", "SecurePass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
    @Test
    void login_withValidEmail_returnsTokenAndUserInfo() {
        authService.register(buildRegisterRequest("login@test.com", null, "MyPassword1!"));
        LoginRequest request = buildLoginRequest("login@test.com", null, "MyPassword1!");
        LoginResponse response = authService.login(request);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());
        assertEquals("login@test.com", response.getUser().getEmail());
    }

    @Test
    void login_withValidPhone_returnsToken() {
        authService.register(buildRegisterRequest(null, "5141110010", "MyPassword1!"));
        LoginRequest request = buildLoginRequest(null, "5141110010", "MyPassword1!");
        LoginResponse response = authService.login(request);
        assertNotNull(response.getToken());
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        authService.register(buildRegisterRequest("wrongpass@test.com", null, "CorrectPass1!"));
        LoginRequest request = buildLoginRequest("wrongpass@test.com", null, "WrongPass!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        LoginRequest request = buildLoginRequest("ghost@test.com", null, "AnyPass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_withUnknownPhone_throwsUnauthorized() {
        LoginRequest request = buildLoginRequest(null, "5140000000", "AnyPass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_withNoCredentials_throwsBadRequest() {
        LoginRequest request = buildLoginRequest(null, null, "AnyPass1!");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void login_normalizesEmailBeforeLookup() {
        authService.register(buildRegisterRequest("normalize@test.com", null, "Pass1!"));
        LoginRequest request = buildLoginRequest("NORMALIZE@TEST.COM", null, "Pass1!");
        LoginResponse response = authService.login(request);
        assertNotNull(response.getToken());
    }

    private RegisterRequest buildRegisterRequest(String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Auth User");
        r.setEmail(email);
        r.setPhoneNumber(phone);
        r.setPassword(password);
        return r;
    }

    private LoginRequest buildLoginRequest(String email, String phone, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPhoneNumber(phone);
        r.setPassword(password);
        return r;
    }

    private User buildUser(String email, String phone) {
        return new User("Alice Smith", email, phone, "hashed-password", UserRole.CUSTOMER);
    }
}
