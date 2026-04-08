package com.pestcontrol.backend.api.dto;

public class CreateReservationRequest {
    private Long eventId;
    private Integer quantity;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
