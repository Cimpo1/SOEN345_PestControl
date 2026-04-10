package com.pestcontrol.backend.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.TicketStatus;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationSystemTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    @MockitoBean private JavaMailSender javaMailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void shouldInstantiateReservationSystemTestClass() {
        assertDoesNotThrow(ReservationSystemTest::new);
    }

    @Test
    void reserve_withValidToken_returns201AndPersistsReservationAndTickets() throws Exception {
        Event event = savedEvent("Reserve Me", Category.CONCERT, "2026-10-01T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 2);

        String responseJson = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interactionStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.ticketCount").value(2))
                .andReturn().getResponse().getContentAsString();

        Long reservationId = objectMapper.readTree(responseJson).get("reservationId").asLong();

        Reservation savedReservation = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, savedReservation.getStatus(),
                "Reservation must be CONFIRMED in DB");
        assertEquals(defaultUser.getUserId(), savedReservation.getUser().getUserId(),
                "Reservation must be linked to the correct user in DB");
        assertEquals(event.getEventId(), savedReservation.getEvent().getEventId(),
                "Reservation must be linked to the correct event in DB");

        List<Ticket> tickets = ticketRepository.findByReservation(savedReservation);
        assertEquals(2, tickets.size(),
                "Ticket count in DB must match requested quantity");
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == TicketStatus.ISSUED),
                "All tickets must have ISSUED status in DB after reservation");
    }

    @Test
    void reserve_withInvalidToken_returns401AndDoesNotPersist() throws Exception {
        Event event = savedEvent("Bad Token Event", Category.CONCERT, "2026-10-03T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer this.is.not.valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());

        assertEquals(0, reservationRepository.count(),
                "Invalid token must not persist a reservation in DB");
        assertEquals(0, ticketRepository.count(),
                "Invalid token must not persist any tickets in DB");
    }

    @Test
    void reserve_withCancelledEvent_returns400AndDoesNotPersist() throws Exception {
        Event event = savedEvent("Cancelled Event", Category.CONCERT, "2026-10-04T20:00:00Z", EventStatus.CANCELLED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 1);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + defaultToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        assertEquals(0, reservationRepository.count(),
                "Reservation for a cancelled event must not be persisted in DB");
    }

    @Test
    void reserve_twice_returns409AndDoesNotCreateDuplicate() throws Exception {
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

        assertEquals(1, reservationRepository.count(),
                "Duplicate reservation attempt must not create a second row in DB");
        assertEquals(1, ticketRepository.count(),
                "Duplicate reservation attempt must not create extra tickets in DB");
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

        List<Reservation> reservations = reservationRepository.findByUserAndStatus(
                defaultUser, ReservationStatus.CONFIRMED);
        assertEquals(1, reservations.size(),
                "Exactly 1 CONFIRMED reservation must exist in DB for current user");
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

        Reservation inDb = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, inDb.getStatus(),
                "Reservation must be CANCELLED in DB");
        List<Ticket> tickets = ticketRepository.findByReservation(inDb);
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == TicketStatus.VOIDED),
                "All tickets must be VOIDED in DB after cancellation");
    }

    @Test
    void cancelReservation_withValidToken_returns200AndSetsStatusAndVoidsTicketsInDB() throws Exception {
        Event event = savedEvent("Cancel Me", Category.CONCERT, "2026-11-15T20:00:00Z", EventStatus.SCHEDULED);
        Map<String, Object> body = Map.of("eventId", event.getEventId(), "quantity", 3);

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

        Reservation inDb = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, inDb.getStatus(),
                "Reservation must be CANCELLED in DB after user cancellation");

        List<Ticket> tickets = ticketRepository.findByReservation(inDb);
        assertEquals(3, tickets.size(),
                "All 3 tickets must still exist in DB (not deleted)");
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == TicketStatus.VOIDED),
                "All tickets must be VOIDED in DB after cancellation");
    }

    @Test
    void cancelReservation_whenOtherUserCancels_returns403AndDoesNotMutateDB() throws Exception {
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

        Reservation inDb = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, inDb.getStatus(),
                "Unauthorized cancel attempt must not change reservation status in DB");
        List<Ticket> tickets = ticketRepository.findByReservation(inDb);
        assertTrue(tickets.stream().allMatch(t -> t.getStatus() == TicketStatus.ISSUED),
                "Unauthorized cancel attempt must not void tickets in DB");
    }

    @Test
    void cancelReservation_whenAlreadyCancelled_returns409AndDoesNotMutateDB() throws Exception {
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

        Reservation inDb = reservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, inDb.getStatus(),
                "Double-cancel must leave reservation CANCELLED in DB without further mutation");
    }

    @Test
    void cancelReservation_whenNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/reservations/{id}", 99999L)
                        .header("Authorization", "Bearer " + defaultToken))
                .andExpect(status().isNotFound());

        assertEquals(0, reservationRepository.count());
    }

    private Event savedEvent(String title, Category category, String start, EventStatus status) {
        OffsetDateTime startDateTime = OffsetDateTime.parse(start);
        return eventRepository.save(new Event(
                defaultLocation, title,
                startDateTime, startDateTime.plusHours(3),
                category, new BigDecimal("49.99"), status));
    }
}