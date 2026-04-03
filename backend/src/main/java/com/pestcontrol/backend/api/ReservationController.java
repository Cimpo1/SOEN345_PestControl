package com.pestcontrol.backend.api;

import com.pestcontrol.backend.api.dto.CreateReservationRequest;
import com.pestcontrol.backend.api.dto.ReservationResponse;
import com.pestcontrol.backend.service.JWTService;
import com.pestcontrol.backend.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateReservationRequest request) {
        Long userId = resolveUserId(authorization);
        ReservationResponse response = reservationService.reserve(userId, request.getEventId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current")
    public ResponseEntity<List<ReservationResponse>> getCurrentReservations(
            @RequestHeader("Authorization") String authorization) {
        Long userId = resolveUserId(authorization);
        return ResponseEntity.ok(reservationService.getCurrentReservations(userId));
    }

    @GetMapping("/interacted")
    public ResponseEntity<List<ReservationResponse>> getInteractedEvents(
            @RequestHeader("Authorization") String authorization) {
        Long userId = resolveUserId(authorization);
        return ResponseEntity.ok(reservationService.getInteractedEvents(userId));
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long reservationId) {
        Long userId = resolveUserId(authorization);
        return ResponseEntity.ok(reservationService.cancel(userId, reservationId));
    }

    private Long resolveUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorization.substring(7);
        if (!JWTService.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        return JWTService.getUserId(token);
    }
}
