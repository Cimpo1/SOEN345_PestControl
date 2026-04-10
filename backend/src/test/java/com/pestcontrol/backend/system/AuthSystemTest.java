package com.pestcontrol.backend.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthSystemTest {

    @Test
    void shouldInstantiateAuthSystemTestClass() {
        assertDoesNotThrow(AuthSystemTest::new);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        ticketRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        User defaultUser = userRepository.save(new User(
                "Test User", "testuser@test.com", "5140001001",
                passwordEncoder.encode("TestPass1!"), UserRole.CUSTOMER));
        JWTService.generateToken(defaultUser);
    }

    @Test
    void register_withValidEmail_returns200AndPersistsUser() throws Exception {
        RegisterRequest request = buildRegisterRequest("new@test.com", null, "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Optional<User> saved = userRepository.findByEmail("new@test.com");
        assertTrue(saved.isPresent(), "User should be persisted in DB");
        assertEquals(UserRole.CUSTOMER, saved.get().getUserRole());
        assertEquals("Test User", saved.get().getFullName());

        assertTrue(passwordEncoder.matches("Pass1!", saved.get().getPasswordHash()),
                "Password should be stored as a valid hash");
    }

    @Test
    void register_withValidPhone_returns200AndPersistsUser() throws Exception {
        RegisterRequest request = buildRegisterRequest(null, "5149990001", "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Optional<User> saved = userRepository.findByPhoneNumber("5149990001");
        assertTrue(saved.isPresent(), "User registered by phone should be persisted in DB");
        assertEquals(UserRole.CUSTOMER, saved.get().getUserRole());
    }

    @Test
    void register_withDuplicateEmail_returns409AndDoesNotCreateExtraUser() throws Exception {
        long countBefore = userRepository.count();
        RegisterRequest request = buildRegisterRequest("testuser@test.com", null, "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertEquals(countBefore, userRepository.count(),
                "Duplicate registration must not create an additional user in DB");
    }

    @Test
    void register_withNoEmailOrPhone_returns400AndDoesNotCreateUser() throws Exception {
        long countBefore = userRepository.count();
        RegisterRequest request = buildRegisterRequest(null, null, "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(countBefore, userRepository.count(),
                "Invalid registration must not persist any user in DB");
    }

    @Test
    void login_withValidCredentials_returnsTokenAndUser() throws Exception {
        LoginRequest request = buildLoginRequest("testuser@test.com", null, "TestPass1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("testuser@test.com"));

        Optional<User> user = userRepository.findByEmail("testuser@test.com");
        assertTrue(user.isPresent());
        assertEquals(UserRole.CUSTOMER, user.get().getUserRole(),
                "Login must not change user role in DB");
    }

    @Test
    void login_withWrongPassword_returns401AndDoesNotMutateUser() throws Exception {
        LoginRequest request = buildLoginRequest("testuser@test.com", null, "WrongPass!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        User user = userRepository.findByEmail("testuser@test.com").orElseThrow();
        assertTrue(passwordEncoder.matches("TestPass1!", user.getPasswordHash()),
                "Failed login attempt must not modify the stored password hash in DB");
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        LoginRequest request = buildLoginRequest("ghost@test.com", null, "AnyPass!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertTrue(userRepository.findByEmail("ghost@test.com").isEmpty(),
                "Unknown login attempt must not create a user in DB");
    }

    private RegisterRequest buildRegisterRequest(String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Test User");
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
}