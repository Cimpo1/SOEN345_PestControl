package com.pestcontrol.backend.service;

import com.pestcontrol.backend.api.dto.CreateEventRequest;
import com.pestcontrol.backend.api.dto.EventLocationRequest;
import com.pestcontrol.backend.api.dto.EventResponse;
import com.pestcontrol.backend.api.dto.UpdateEventRequest;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.TicketStatus;
import com.pestcontrol.backend.infrastructure.repositories.EventRepository;
import com.pestcontrol.backend.infrastructure.repositories.LocationRepository;
import com.pestcontrol.backend.infrastructure.repositories.ReservationRepository;
import com.pestcontrol.backend.infrastructure.repositories.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class EventService {
    private static final String EVENT_NOT_FOUND_MESSAGE = "Event not found";
    private static final String TITLE_REQUIRED_MESSAGE = "title is required";

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final ReservationEmailService reservationEmailService;
    private final Clock clock;

    @Autowired
    public EventService(
            EventRepository eventRepository,
            LocationRepository locationRepository,
            ReservationRepository reservationRepository,
            TicketRepository ticketRepository,
            ReservationEmailService reservationEmailService) {
        this(
                eventRepository,
                locationRepository,
                reservationRepository,
                ticketRepository,
                reservationEmailService,
                Clock.systemUTC());
    }

    EventService(
            EventRepository eventRepository,
            LocationRepository locationRepository,
            ReservationRepository reservationRepository,
            TicketRepository ticketRepository,
            ReservationEmailService reservationEmailService,
            Clock clock) {
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
        this.reservationEmailService = reservationEmailService;
        this.clock = Objects.requireNonNullElse(clock, Clock.systemUTC());
    }

    @Transactional
    public List<EventResponse> getEvents(
            String title,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            List<Category> categories) {
        refreshPastStatuses();

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

    @Transactional
    public EventResponse getEventById(Long eventId) {
        refreshPastStatuses();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND_MESSAGE));

        return new EventResponse(event);
    }

    @Transactional
    public List<EventResponse> getAdminEvents(String status) {
        refreshPastStatuses();

        List<Event> events;
        if (status == null || status.isBlank()) {
            events = eventRepository.findAll();
        } else {
            events = eventRepository.findByStatus(parseEventStatus(status));
        }

        return events.stream()
                .sorted(Comparator.comparing(Event::getStartDateTime).reversed())
                .map(EventResponse::new)
                .toList();
    }

    @Transactional
    public EventResponse createEvent(Long adminUserId, CreateEventRequest request) {
        validateRequest(request);

        Location location = resolveLocation(request.getLocationId(), request.getLocation());
        Event event = new Event(
                location,
                normalizeRequired(request.getTitle(), TITLE_REQUIRED_MESSAGE),
                request.getStartDateTime(),
                request.getEndDateTime(),
                parseCategory(request.getCategory()),
                request.getBasePrice(),
                EventStatus.SCHEDULED);

        Event saved = eventRepository.save(event);
        return new EventResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long adminUserId, Long eventId, UpdateEventRequest request) {
        validateRequest(request);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND_MESSAGE));

        refreshPastStatuses();
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only available events can be edited");
        }

        Location location = resolveLocationForUpdate(event, request.getLocationId(), request.getLocation());
        OffsetDateTime previousStartDateTime = event.getStartDateTime();
        OffsetDateTime previousEndDateTime = event.getEndDateTime();
        boolean timeChanged = !previousStartDateTime.equals(request.getStartDateTime())
                || !previousEndDateTime.equals(request.getEndDateTime());

        event.setTitle(normalizeRequired(request.getTitle(), TITLE_REQUIRED_MESSAGE));
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setCategory(parseCategory(request.getCategory()));
        event.setBasePrice(request.getBasePrice());
        event.setLocation(location);

        Event saved = eventRepository.save(event);

        if (timeChanged) {
            notifyActiveReservationsAboutTimeChange(
                    saved,
                    previousStartDateTime,
                    previousEndDateTime);
        }

        return new EventResponse(saved);
    }

    @Transactional
    public EventResponse cancelEvent(Long adminUserId, Long eventId) {
        refreshPastStatuses();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EVENT_NOT_FOUND_MESSAGE));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is already cancelled");
        }

        if (event.getStatus() == EventStatus.PAST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Past events cannot be cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
        Event savedEvent = eventRepository.save(event);

        List<Reservation> reservations = reservationRepository.findByEvent(savedEvent);
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() != ReservationStatus.CANCELLED) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                reservationRepository.save(reservation);
            }

            List<Ticket> tickets = ticketRepository.findByReservation(reservation);
            for (Ticket ticket : tickets) {
                ticket.setStatus(TicketStatus.VOIDED);
            }
            if (!tickets.isEmpty()) {
                ticketRepository.saveAll(tickets);
            }

            if (reservation.getUser().getEmail() != null) {
                reservationEmailService.sendEventCancellationConfirmation(
                        reservation.getUser().getEmail(),
                        reservation,
                        tickets);
            }
        }

        return new EventResponse(savedEvent);
    }

    private boolean matchesTitle(Event event, String title) {
        if (title == null) {
            return true;
        }
        return event.getTitle() != null && event.getTitle().toLowerCase().contains(title);
    }

    private boolean matchesDateRange(Event event, LocalDate startDate, LocalDate endDate) {
        LocalDate eventDate = event.getStartDateTime().toLocalDate();

        return (startDate == null || !eventDate.isBefore(startDate))
                && (endDate == null || !eventDate.isAfter(endDate));
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

    private void refreshPastStatuses() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Event> scheduledEvents = eventRepository.findByStatus(EventStatus.SCHEDULED);
        List<Event> endedEvents = scheduledEvents.stream()
                .filter(event -> !event.getEndDateTime().isAfter(now))
                .toList();

        if (endedEvents.isEmpty()) {
            return;
        }

        for (Event event : endedEvents) {
            event.setStatus(EventStatus.PAST);
        }
        eventRepository.saveAll(endedEvents);
    }

    private void validateRequest(CreateEventRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        validateDatesAndPrice(request.getStartDateTime(), request.getEndDateTime(), request.getBasePrice());
        normalizeRequired(request.getTitle(), TITLE_REQUIRED_MESSAGE);
        parseCategory(request.getCategory());
        ensureLocationPayload(request.getLocationId(), request.getLocation());
    }

    private void validateRequest(UpdateEventRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        validateDatesAndPrice(request.getStartDateTime(), request.getEndDateTime(), request.getBasePrice());
        normalizeRequired(request.getTitle(), TITLE_REQUIRED_MESSAGE);
        parseCategory(request.getCategory());
        ensureLocationPayload(request.getLocationId(), request.getLocation());
    }

    private void validateDatesAndPrice(OffsetDateTime startDateTime, OffsetDateTime endDateTime, BigDecimal basePrice) {
        if (startDateTime == null || endDateTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDateTime and endDateTime are required");
        }
        if (!endDateTime.isAfter(startDateTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDateTime must be after startDateTime");
        }
        if (!startDateTime.isAfter(OffsetDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDateTime must be in the future");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "basePrice must be greater than 0");
        }
    }

    private Category parseCategory(String categoryText) {
        String normalized = normalizeRequired(categoryText, "category is required");
        try {
            return Category.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category value");
        }
    }

    private EventStatus parseEventStatus(String statusText) {
        try {
            return EventStatus.valueOf(statusText.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
        }
    }

    private Location resolveLocation(Long locationId, EventLocationRequest locationRequest) {
        if (locationId != null) {
            return locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
        }

        return createLocation(locationRequest);
    }

    private Location resolveLocationForUpdate(Event event, Long locationId, EventLocationRequest locationRequest) {
        if (locationId != null || locationRequest != null) {
            return resolveLocation(locationId, locationRequest);
        }
        return event.getLocation();
    }

    private Location createLocation(EventLocationRequest locationRequest) {
        if (locationRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either locationId or location must be provided");
        }

        Location location = new Location(
                normalizeRequired(locationRequest.getName(), "location.name is required"),
                normalizeRequired(locationRequest.getAddressLine(), "location.addressLine is required"),
                normalizeRequired(locationRequest.getCity(), "location.city is required"),
                normalizeRequired(locationRequest.getProvince(), "location.province is required"),
                normalizeRequired(locationRequest.getPostalCode(), "location.postalCode is required"));
        return locationRepository.save(location);
    }

    private void ensureLocationPayload(Long locationId, EventLocationRequest locationRequest) {
        if (locationId == null && locationRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either locationId or location must be provided");
        }
    }

    private void notifyActiveReservationsAboutTimeChange(
            Event event,
            OffsetDateTime previousStartDateTime,
            OffsetDateTime previousEndDateTime) {
        List<Reservation> reservations = reservationRepository.findByEvent(event);

        String oldStartTime = previousStartDateTime
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"));
        String oldEndTime = previousEndDateTime
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"));

        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            List<Ticket> tickets = ticketRepository.findByReservation(reservation);
            if (reservation.getUser().getEmail() != null) {
                reservationEmailService.sendEventTimeUpdatedConfirmation(
                        reservation.getUser().getEmail(),
                        reservation,
                        tickets,
                        oldStartTime,
                        oldEndTime);
            }
        }
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        return trimmed;
    }
}
