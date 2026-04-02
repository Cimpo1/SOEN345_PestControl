package com.pestcontrol.backend.unit.api;

import com.pestcontrol.backend.api.ReservationController;
import com.pestcontrol.backend.api.dto.CreateReservationRequest;
import com.pestcontrol.backend.api.dto.ReservationResponse;
import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.service.JWTService;
import com.pestcontrol.backend.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    @Test
    void reserve_withValidToken_shouldDelegateAndReturnCreated() {
        String token = createToken(15L);
        String authorization = "Bearer " + token;
        CreateReservationRequest request = new CreateReservationRequest();
        request.setEventId(5L);

        ReservationResponse mockResponse = createResponse(44L, "REGISTERED");
        when(reservationService.reserve(15L, 5L)).thenReturn(mockResponse);

        ResponseEntity<ReservationResponse> response = reservationController.reserve(authorization, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(44L, response.getBody().getReservationId());
        verify(reservationService).reserve(15L, 5L);
    }

    @Test
    void getInteractedEvents_withInvalidHeader_shouldThrowUnauthorized() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationController.getInteractedEvents("invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void getCurrentReservations_shouldDelegateToService() {
        String token = createToken(15L);
        String authorization = "Bearer " + token;
        when(reservationService.getCurrentReservations(15L)).thenReturn(List.of(createResponse(1L, "REGISTERED")));

        ResponseEntity<List<ReservationResponse>> response = reservationController
                .getCurrentReservations(authorization);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(reservationService).getCurrentReservations(15L);
    }

    private String createToken(Long userId) {
        User user = new User("Customer", "customer@test.com", "5145550000", "hash", UserRole.CUSTOMER);
        user.setUserId(userId);
        return JWTService.generateToken(user);
    }

    private ReservationResponse createResponse(Long reservationId, String interactionStatus) {
        Location location = new Location("Bell Centre", "1 Arena", "Montreal", "QC", "H1A1A1");
        Event event = new Event(
                location,
                "Test Event",
                OffsetDateTime.parse("2026-04-03T18:00:00Z"),
                OffsetDateTime.parse("2026-04-03T21:00:00Z"),
                Category.CONCERT,
                new BigDecimal("59.99"),
                EventStatus.SCHEDULED);

        Reservation reservation = new Reservation(
                null,
                event,
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));
        reservation.setReservationId(reservationId);

        return new ReservationResponse(reservation, interactionStatus);
    }
}
