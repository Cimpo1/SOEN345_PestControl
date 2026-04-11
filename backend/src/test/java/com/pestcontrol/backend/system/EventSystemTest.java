package com.pestcontrol.backend.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventLocationRequest;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.LocationRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventSystemTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @MockitoBean private JavaMailSender javaMailSender;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private MockMvc mockMvc;
    private Location defaultLocation;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        ticketRepository.deleteAll();
        reservationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        User customerUser = userRepository.save(new User(
                "Test User", "testuser@test.com", "5140001001",
                passwordEncoder.encode("TestPass1!"), UserRole.CUSTOMER));
        JWTService.generateToken(customerUser);

        User adminUser = userRepository.save(new User(
                "Admin User", "admin@test.com", "5140001002",
                passwordEncoder.encode("AdminPass1!"), UserRole.ADMIN));
        adminToken = JWTService.generateToken(adminUser);

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

        assertEquals(2, eventRepository.count());

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
    void createEvent_withAdminTokenAndExistingLocation_returns201AndPersistsEvent() throws Exception {
        long countBefore = eventRepository.count();

        MvcResult result = mockMvc.perform(post("/events/admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(defaultLocation.getLocationId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Event"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        assertEquals(countBefore + 1, eventRepository.count(),
                "Event count in DB should increase by 1 after creation");

        Long eventId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("eventId").asLong();
        Optional<Event> saved = eventRepository.findById(eventId);
        assertTrue(saved.isPresent(), "Created event must be findable in DB by its returned ID");
        assertEquals("New Event", saved.get().getTitle());
        assertEquals(EventStatus.SCHEDULED, saved.get().getStatus());
        assertEquals(defaultLocation.getLocationId(), saved.get().getLocation().getLocationId());
    }

    @Test
    void createEvent_withInlineLocation_returns201AndPersistsBothEventAndLocation() throws Exception {
        long locationCountBefore = locationRepository.count();

        mockMvc.perform(post("/events/admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequestWithInlineLocation())))
                .andExpect(status().isCreated());

        assertEquals(locationCountBefore + 1, locationRepository.count(),
                "Inline location should be persisted as a new Location row in DB");
        assertEquals(1, eventRepository.count(),
                "One event should be persisted in DB");
    }

    @Test
    void createEvent_withoutAdminToken_returns401AndDoesNotPersist() throws Exception {
        long countBefore = eventRepository.count();

        mockMvc.perform(post("/events/admin")
                        .header("Authorization", "Bearer <invalid-or-valid-token>")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(defaultLocation.getLocationId()))))
                .andExpect(status().isUnauthorized());

        assertEquals(countBefore, eventRepository.count(),
                "Unauthorized request must not persist any event in DB");
    }

    @Test
    void updateEvent_withAdminToken_returns200AndUpdatesDB() throws Exception {
        Event existing = savedEvent("Old Title", Category.CONCERT, "2026-11-01T20:00:00Z", EventStatus.SCHEDULED);

        UpdateEventRequest updateRequest = buildUpdateRequest(defaultLocation.getLocationId());
        updateRequest.setTitle("Updated Title");

        mockMvc.perform(put("/events/admin/{id}", existing.getEventId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        Event inDb = eventRepository.findById(existing.getEventId()).orElseThrow();
        assertEquals("Updated Title", inDb.getTitle(),
                "Event title must be updated in DB after PUT");
        assertEquals(Category.SPORTS, inDb.getCategory(),
                "Event category must be updated in DB after PUT");
    }

    @Test
    void updateEvent_onCancelledEvent_returns409AndDoesNotMutateDB() throws Exception {
        Event cancelled = savedEvent("Cancelled", Category.CONCERT, "2026-11-01T20:00:00Z", EventStatus.CANCELLED);

        mockMvc.perform(put("/events/admin/{id}", cancelled.getEventId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest(defaultLocation.getLocationId()))))
                .andExpect(status().isConflict());

        Event inDb = eventRepository.findById(cancelled.getEventId()).orElseThrow();
        assertEquals("Cancelled", inDb.getTitle(),
                "Failed update must not mutate the event title in DB");
        assertEquals(EventStatus.CANCELLED, inDb.getStatus(),
                "Failed update must not change event status in DB");
    }

    @Test
    void cancelEvent_scheduledEvent_returns200AndSetsCancelledStatusInDB() throws Exception {
        Event event = savedEvent("To Cancel", Category.CONCERT, "2026-12-01T20:00:00Z", EventStatus.SCHEDULED);

        mockMvc.perform(delete("/events/admin/{id}", event.getEventId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Event inDb = eventRepository.findById(event.getEventId()).orElseThrow();
        assertEquals(EventStatus.CANCELLED, inDb.getStatus(),
                "Event status must be CANCELLED in DB after admin cancel");
    }

    @Test
    void cancelEvent_alreadyCancelled_returns409AndDoesNotMutateDB() throws Exception {
        Event event = savedEvent("Already Cancelled", Category.CONCERT, "2026-12-01T20:00:00Z", EventStatus.CANCELLED);

        mockMvc.perform(delete("/events/admin/{id}", event.getEventId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        Event inDb = eventRepository.findById(event.getEventId()).orElseThrow();
        assertEquals(EventStatus.CANCELLED, inDb.getStatus(),
                "Double-cancel must not further mutate the event in DB");
    }

    private Event savedEvent(String title, Category category, String start, EventStatus status) {
        OffsetDateTime startDateTime = OffsetDateTime.parse(start);
        return eventRepository.save(new Event(
                defaultLocation, title,
                startDateTime, startDateTime.plusHours(3),
                category, new BigDecimal("49.99"), status));
    }

    private CreateEventRequest buildCreateRequest(Long locationId) {
        CreateEventRequest r = new CreateEventRequest();
        r.setTitle("New Event");
        r.setStartDateTime(OffsetDateTime.now().plusDays(5));
        r.setEndDateTime(OffsetDateTime.now().plusDays(5).plusHours(3));
        r.setCategory("SPORTS");
        r.setBasePrice(BigDecimal.valueOf(50.00));
        r.setLocationId(locationId);
        return r;
    }

    private CreateEventRequest buildCreateRequestWithInlineLocation() {
        EventLocationRequest loc = new EventLocationRequest();
        loc.setName("New Venue");
        loc.setAddressLine("456 Rue Sainte-Catherine");
        loc.setCity("Montreal");
        loc.setProvince("QC");
        loc.setPostalCode("H3B 1A7");

        CreateEventRequest r = buildCreateRequest(null);
        r.setLocation(loc);
        return r;
    }

    private UpdateEventRequest buildUpdateRequest(Long locationId) {
        UpdateEventRequest r = new UpdateEventRequest();
        r.setTitle("Updated Title");
        r.setStartDateTime(OffsetDateTime.now().plusDays(6));
        r.setEndDateTime(OffsetDateTime.now().plusDays(6).plusHours(3));
        r.setCategory("SPORTS");
        r.setBasePrice(BigDecimal.valueOf(75.00));
        r.setLocationId(locationId);
        return r;
    }
}