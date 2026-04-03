package com.pestcontrol.backend.api.dto;

import com.pestcontrol.backend.domain.Reservation;

import java.time.OffsetDateTime;

public class ReservationResponse {
    private Long reservationId;
    private String reservationStatus;
    private String interactionStatus;
    private OffsetDateTime creationDate;
    private EventResponse event;

    public ReservationResponse(Reservation reservation, String interactionStatus) {
        this.reservationId = reservation.getReservationId();
        this.reservationStatus = reservation.getStatus().name();
        this.interactionStatus = interactionStatus;
        this.creationDate = reservation.getCreationDate();
        this.event = new EventResponse(reservation.getEvent());
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public String getInteractionStatus() {
        return interactionStatus;
    }

    public void setInteractionStatus(String interactionStatus) {
        this.interactionStatus = interactionStatus;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public EventResponse getEvent() {
        return event;
    }

    public void setEvent(EventResponse event) {
        this.event = event;
    }
}
