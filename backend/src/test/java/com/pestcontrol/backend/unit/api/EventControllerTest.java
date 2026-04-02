package com.pestcontrol.backend.unit.api;

import com.pestcontrol.backend.api.EventController;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
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
        when(eventService.getEvents(
                eq("jazz"),
                eq(LocalDate.parse("2026-07-01")),
                eq(LocalDate.parse("2026-07-31")),
                eq("Montreal"),
                eq(List.of(Category.CONCERT, Category.SPORTS)))).thenReturn(List.of());

        ResponseEntity<List<EventResponse>> response = eventController.getEvents(
                "jazz",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-31"),
                "Montreal",
                "concert,sports");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventService).getEvents(
                "jazz",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-31"),
                "Montreal",
                List.of(Category.CONCERT, Category.SPORTS));
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
}