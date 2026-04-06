package com.pestcontrol.backend.unit.api;

import com.pestcontrol.backend.api.EventController;
import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.service.JWTService;
import com.pestcontrol.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void getEvents_shouldParseCategoriesAndDelegateToService() {
        LocalDate startDate = LocalDate.parse("2026-07-01");
        LocalDate endDate = LocalDate.parse("2026-07-31");
        List<Category> categories = List.of(Category.CONCERT, Category.SPORTS);

        when(eventService.getEvents(
                "jazz",
                startDate,
                endDate,
                "Montreal",
                categories)).thenReturn(List.of());

        ResponseEntity<List<EventResponse>> response = eventController.getEvents(
                "jazz",
                startDate,
                endDate,
                "Montreal",
                "concert,sports");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventService).getEvents(
                "jazz",
                startDate,
                endDate,
                "Montreal",
                categories);
    }

    @Test
    void getEvents_whenCategoryIsInvalid_shouldThrowBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getEvents(null, null, null, null, "concert,unknown"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getEventById_shouldDelegateToService() {
        EventResponse mockResponse = org.mockito.Mockito.mock(EventResponse.class);
        when(eventService.getEventById(5L)).thenReturn(mockResponse);

        ResponseEntity<EventResponse> response = eventController.getEventById(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(eventService).getEventById(5L);
    }

    @Test
    void getAdminEvents_whenAuthorizationHeaderMissingBearer_shouldThrowUnauthorized() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> eventController.getAdminEvents("invalid-header", null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void getAdminEvents_whenValidAdminToken_shouldDelegateToService() {
        try (MockedStatic<JWTService> jwtService = mockStatic(JWTService.class)) {
            jwtService.when(() -> JWTService.validateToken("valid-token")).thenReturn(true);
            jwtService.when(() -> JWTService.getRole("valid-token")).thenReturn("ADMIN");
            jwtService.when(() -> JWTService.getUserId("valid-token")).thenReturn(10L);

            when(eventService.getAdminEvents(eq("SCHEDULED"))).thenReturn(List.of());

            ResponseEntity<List<EventResponse>> response = eventController.getAdminEvents(
                    "Bearer valid-token",
                    "SCHEDULED");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(eventService).getAdminEvents("SCHEDULED");
        }
    }

    @Test
    void createEvent_whenValidAdminToken_shouldCreateEvent() {
        try (MockedStatic<JWTService> jwtService = mockStatic(JWTService.class)) {
            jwtService.when(() -> JWTService.validateToken("valid-token")).thenReturn(true);
            jwtService.when(() -> JWTService.getRole("valid-token")).thenReturn("ADMIN");
            jwtService.when(() -> JWTService.getUserId("valid-token")).thenReturn(10L);

            CreateEventRequest request = new CreateEventRequest();
            when(eventService.createEvent(eq(10L), any(CreateEventRequest.class)))
                    .thenReturn(org.mockito.Mockito.mock(EventResponse.class));

            ResponseEntity<EventResponse> response = eventController.createEvent("Bearer valid-token", request);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            verify(eventService).createEvent(eq(10L), any(CreateEventRequest.class));
        }
    }

    @Test
    void updateEvent_whenValidAdminToken_shouldUpdateEvent() {
        try (MockedStatic<JWTService> jwtService = mockStatic(JWTService.class)) {
            jwtService.when(() -> JWTService.validateToken("valid-token")).thenReturn(true);
            jwtService.when(() -> JWTService.getRole("valid-token")).thenReturn("ADMIN");
            jwtService.when(() -> JWTService.getUserId("valid-token")).thenReturn(10L);

            UpdateEventRequest request = new UpdateEventRequest();
            when(eventService.updateEvent(eq(10L), eq(99L), any(UpdateEventRequest.class)))
                    .thenReturn(org.mockito.Mockito.mock(EventResponse.class));

            ResponseEntity<EventResponse> response = eventController.updateEvent(
                    "Bearer valid-token",
                    99L,
                    request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(eventService).updateEvent(eq(10L), eq(99L), any(UpdateEventRequest.class));
        }
    }

    @Test
    void cancelEvent_whenValidAdminToken_shouldCancelEvent() {
        try (MockedStatic<JWTService> jwtService = mockStatic(JWTService.class)) {
            jwtService.when(() -> JWTService.validateToken("valid-token")).thenReturn(true);
            jwtService.when(() -> JWTService.getRole("valid-token")).thenReturn("ADMIN");
            jwtService.when(() -> JWTService.getUserId("valid-token")).thenReturn(10L);

            when(eventService.cancelEvent(10L, 99L)).thenReturn(org.mockito.Mockito.mock(EventResponse.class));

            ResponseEntity<EventResponse> response = eventController.cancelEvent("Bearer valid-token", 99L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(eventService).cancelEvent(10L, 99L);
        }
    }
}