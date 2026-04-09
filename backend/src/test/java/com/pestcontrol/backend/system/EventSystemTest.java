package com.pestcontrol.backend.system;

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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventSystemTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    private Location defaultLocation;

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

        defaultLocation = new Location("Bell Centre", "1909 Av des Canadiens", "Montreal", "QC", "H3B 5E8");
        entityManager.persist(defaultLocation);
        entityManager.flush();
    }

    @Test
    void shouldInstantiateEventSystemTestClass() {
        assertDoesNotThrow(EventSystemTest::new);
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

    private Event savedEvent(String title, Category category, String start, EventStatus status) {
        OffsetDateTime startDateTime = OffsetDateTime.parse(start);
        return eventRepository.save(new Event(
                defaultLocation, title,
                startDateTime, startDateTime.plusHours(3),
                category, new BigDecimal("49.99"), status));
    }
}
