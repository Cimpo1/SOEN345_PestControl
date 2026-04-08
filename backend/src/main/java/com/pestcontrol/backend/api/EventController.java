package com.pestcontrol.backend.api;

import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.service.EventService;
import com.pestcontrol.backend.service.JWTService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String categories) {
        List<Category> categoryList = parseCategories(categories);
        List<EventResponse> events = eventService.getEvents(title, startDate, endDate, location, categoryList);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<EventResponse>> getAdminEvents(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String status) {
        resolveAdminUserId(authorization);
        return ResponseEntity.ok(eventService.getAdminEvents(status));
    }

    @PostMapping("/admin")
    public ResponseEntity<EventResponse> createEvent(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateEventRequest request) {
        Long adminUserId = resolveAdminUserId(authorization);
        EventResponse response = eventService.createEvent(adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long eventId,
            @RequestBody UpdateEventRequest request) {
        Long adminUserId = resolveAdminUserId(authorization);
        return ResponseEntity.ok(eventService.updateEvent(adminUserId, eventId, request));
    }

    @DeleteMapping("/admin/{eventId}")
    public ResponseEntity<EventResponse> cancelEvent(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long eventId) {
        Long adminUserId = resolveAdminUserId(authorization);
        return ResponseEntity.ok(eventService.cancelEvent(adminUserId, eventId));
    }

    private List<Category> parseCategories(String categories) {
        if (categories == null || categories.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return List.of(categories.split(",")).stream()
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(String::toUpperCase)
                    .map(Category::valueOf)
                    .distinct()
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category value");
        }
    }

    private Long resolveAdminUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorization.substring(7);
        if (!JWTService.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        String role = JWTService.getRole(token);
        if (!UserRole.ADMIN.name().equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        return JWTService.getUserId(token);
    }
}
