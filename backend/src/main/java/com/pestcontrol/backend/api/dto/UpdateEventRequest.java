package com.pestcontrol.backend.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class UpdateEventRequest {
    private String title;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private String category;
    private BigDecimal basePrice;
    private Long locationId;
    private EventLocationRequest location;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public EventLocationRequest getLocation() {
        return location;
    }

    public void setLocation(EventLocationRequest location) {
        this.location = location;
    }
}