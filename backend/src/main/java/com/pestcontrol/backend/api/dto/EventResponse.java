package com.pestcontrol.backend.api.dto;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class EventResponse {
    private Long eventId;
    private String title;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private Category category;
    private BigDecimal basePrice;
    private EventStatus status;
    private EventLocationResponse location;

    public EventResponse(Event event) {
        this.eventId = event.getEventId();
        this.title = event.getTitle();
        this.startDateTime = event.getStartDateTime();
        this.endDateTime = event.getEndDateTime();
        this.category = event.getCategory();
        this.basePrice = event.getBasePrice();
        this.status = event.getStatus();
        this.location = new EventLocationResponse(event.getLocation());
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventLocationResponse getLocation() {
        return location;
    }

    public void setLocation(EventLocationResponse location) {
        this.location = location;
    }
}