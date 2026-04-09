package com.pestcontrol.backend.integration.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pestcontrol.backend.api.dto.LoginRequest;
import com.pestcontrol.backend.api.dto.RegisterRequest;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import com.pestcontrol.backend.infrastructure.repositories.UserRepository;
import com.pestcontrol.backend.service.JWTService;
import jakarta.persistence.EntityManager;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ControllerIntegrationTest {

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

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private MockMvc mockMvc;
    private User defaultUser;
    private String defaultToken;
    private Location defaultLocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        ticketRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        defaultUser = userRepository.save(new User(
                "Test User", "testuser@test.com", "5140001001",
                passwordEncoder.encode("TestPass1!"), UserRole.CUSTOMER));
        defaultToken = JWTService.generateToken(defaultUser);

        defaultLocation = new Location("Bell Centre", "1909 Av des Canadiens", "Montreal", "QC", "H3B 5E8");
        entityManager.persist(defaultLocation);
        entityManager.flush();
    }

    @Test
    void register_withValidEmail_returns200() throws Exception {
        RegisterRequest request = buildRegisterRequest("new@test.com", null, "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterRequest request = buildRegisterRequest("testuser@test.com", null, "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withNoEmailOrPhone_returns400() throws Exception {
        RegisterRequest request = buildRegisterRequest(null, null, "Pass1!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        LoginRequest request = buildLoginRequest("testuser@test.com", null, "WrongPass!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        LoginRequest request = buildLoginRequest("ghost@test.com", null, "AnyPass!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEvents_withNoFilters_returnsOnlyScheduledEvents() throws Exception {
        savedEvent("Rock Show", Category.CONCERT, "2026-09-01T20:00:00Z", EventStatus.SCHEDULED);
        savedEvent("Cancelled Gig", Category.CONCERT, "2026-09-02T20:00:00Z", EventStatus.CANCELLED);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Rock Show"));
    }

    @Test
    void getEvents_withTitleFilter_returnsMatchingEvents() throws Exception {
        savedEvent("Jazz Festival", Category.CONCERT, "2026-09-01T20:00:00Z", EventStatus.SCHEDULED);
        savedEvent("Comedy Night", Category.COMEDY, "2026-09-02T19:00:00Z", EventStatus.SCHEDULED);

        mockMvc.perform(get("/events").param("title", "jazz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Jazz Festival"));
    }

    @Test
    void getEvents_withCategoryFilter_returnsMatchingEvents() throws Exception {
        savedEvent("Sports Game", Category.SPORTS, "2026-09-01T18:00:00Z", EventStatus.SCHEDULED);
        savedEvent("Art Exhibit", Category.ART_THEATER, "2026-09-02T15:00:00Z", EventStatus.SCHEDULED);

        mockMvc.perform(get("/events").param("categories", "SPORTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("SPORTS"));
    }

    @Test
    void getEvents_withMultipleCategories_returnsAllMatching() throws Exception {
        savedEvent("Comedy Night", Category.COMEDY, "2026-09-01T19:00:00Z", EventStatus.SCHEDULED);
        savedEvent("Art Show", Category.ART_THEATER, "2026-09-02T15:00:00Z", EventStatus.SCHEDULED);
        savedEvent("Sports Game", Category.SPORTS, "2026-09-03T18:00:00Z", EventStatus.SCHEDULED);

        mockMvc.perform(get("/events").param("categories", "COMEDY,ART_THEATER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getEvents_withInvalidCategory_returns400() throws Exception {
        mockMvc.perform(get("/events").param("categories", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_withEndDateBeforeStartDate_returns400() throws Exception {
        mockMvc.perform(get("/events")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-08-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEventById_whenExists_returnsEvent() throws Exception {
        Event event = savedEvent("Solo Show", Category.CONCERT, "2026-10-10T19:00:00Z", EventStatus.SCHEDULED);

        mockMvc.perform(get("/events/{id}", event.getEventId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Solo Show"))
                .andExpect(jsonPath("$.category").value("CONCERT"));
    }

    @Test
    void getEventById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/events/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserve_withValidToken_returns201AndReservation() throws Exception {
        Event event = savedEvent("Reserve Me", Category.CONCERT, "2026-10-01T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 2);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interactionStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.ticketCount").value(2));
    }

    @Test
    void reserve_withInvalidToken_returns401() throws Exception {
        Event event = savedEvent("Bad Token Event", Category.CONCERT, "2026-10-03T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer this.is.not.valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reserve_withCancelledEvent_returns400() throws Exception {
        Event event = savedEvent("Cancelled Event", Category.CONCERT, "2026-10-04T20:00:00Z", EventStatus.CANCELLED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserve_twice_returns409() throws Exception {
        Event event = savedEvent("Double Reserve", Category.CONCERT, "2026-10-05T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);
        String bodyJson = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isConflict());
    }

    @Test
    void getCurrentReservations_withValidToken_returnsOnlyFutureConfirmed() throws Exception {
        Event event = savedEvent("Future Event", Category.CONCERT, "2026-11-01T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        mockMvc.perform(post("/reservations")
                .header("Authorization", "Bearer " + defaultToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        mockMvc.perform(get("/reservations/current")
                        .header("Authorization", "Bearer " + defaultToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].interactionStatus").value("REGISTERED"));
    }

    @Test
    void getInteractedEvents_includesCancelledReservations() throws Exception {
        Event event = savedEvent("Interacted Event", Category.CONCERT, "2026-11-10T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> reserveBody = Map.of("eventId", event.getEventId(), "quantity", 1);

        String reserveResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveBody)))
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(reserveResult).get("reservationId").asLong();

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                .header("Authorization", "Bearer " + defaultToken));

        mockMvc.perform(get("/reservations/interacted")
                        .header("Authorization", "Bearer " + defaultToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].interactionStatus").value("CANCELLED"));
    }

    @Test
    void cancelReservation_withValidToken_returns200AndCancelledStatus() throws Exception {
        Event event = savedEvent("Cancel Me", Category.CONCERT, "2026-11-15T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        String reserveResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(reserveResult).get("reservationId").asLong();

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                        .header("Authorization", "Bearer " + defaultToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interactionStatus").value("CANCELLED"));
    }

    @Test
    void cancelReservation_whenOtherUserCancels_returns403() throws Exception {
        Event event = savedEvent("Forbidden Cancel", Category.CONCERT, "2026-11-20T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        String reserveResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(reserveResult).get("reservationId").asLong();

        User otherUser = userRepository.save(new User(
                "Other User", "other@test.com", "5140001002",
                passwordEncoder.encode("OtherPass1!"), UserRole.CUSTOMER));
        String otherToken = JWTService.generateToken(otherUser);

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelReservation_whenAlreadyCancelled_returns409() throws Exception {
        Event event = savedEvent("Double Cancel", Category.CONCERT, "2026-11-25T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        String reserveResult = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(reserveResult).get("reservationId").asLong();

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                .header("Authorization", "Bearer " + defaultToken));

        mockMvc.perform(delete("/reservations/{id}", reservationId)
                        .header("Authorization", "Bearer " + defaultToken))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelReservation_whenNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/reservations/{id}", 99999L)
                        .header("Authorization", "Bearer " + defaultToken))
                .andExpect(status().isNotFound());
    }

    private Event savedEvent(String title, Category category, String start, EventStatus status) {
        OffsetDateTime startDateTime = OffsetDateTime.parse(start);
        return eventRepository.save(new Event(
                defaultLocation, title,
                startDateTime, startDateTime.plusHours(3),
                category, new BigDecimal("49.99"), status));
    }

    private RegisterRequest buildRegisterRequest(String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.fullName = "Test User";
        r.email = email;
        r.phoneNumber = phone;
        r.password = password;
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