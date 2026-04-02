package com.pestcontrol.backend.service;

import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<EventResponse> getEvents(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            List<Category> categories) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate cannot be before startDate");
        }

        String normalizedTitle = normalize(title);
        String normalizedLocation = normalize(location);

        return eventRepository.findAll().stream()
                .filter(event -> event.getStatus() == EventStatus.SCHEDULED)
                .filter(event -> matchesTitle(event, normalizedTitle))
                .filter(event -> matchesDateRange(event, startDate, endDate))
                .filter(event -> matchesLocation(event, normalizedLocation))
                .filter(event -> matchesCategories(event, categories))
                .sorted(Comparator.comparing(Event::getStartDateTime))
                .map(EventResponse::new)
                .toList();
    }

    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        return new EventResponse(event);
    }

    private boolean matchesTitle(Event event, String title) {
        if (title == null) {
            return true;
        }
        return event.getTitle() != null && event.getTitle().toLowerCase().contains(title);
    }

    private boolean matchesDateRange(Event event, LocalDate startDate, LocalDate endDate) {
        LocalDate eventDate = event.getStartDateTime().toLocalDate();

        if (startDate != null && eventDate.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && eventDate.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    private boolean matchesLocation(Event event, String locationQuery) {
        if (locationQuery == null) {
            return true;
        }

        String name = normalize(event.getLocation().getName());
        String city = normalize(event.getLocation().getCity());
        String province = normalize(event.getLocation().getProvince());

        return contains(name, locationQuery) || contains(city, locationQuery) || contains(province, locationQuery);
    }

    private boolean matchesCategories(Event event, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }

        return categories.contains(event.getCategory());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean contains(String fieldValue, String query) {
        return fieldValue != null && fieldValue.contains(query);
    }
}
