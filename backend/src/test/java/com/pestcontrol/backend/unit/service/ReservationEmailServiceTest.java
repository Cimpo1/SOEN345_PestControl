package com.pestcontrol.backend.unit.service;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import com.pestcontrol.backend.domain.enums.Category;
import com.pestcontrol.backend.domain.enums.EventStatus;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import com.pestcontrol.backend.domain.enums.TicketStatus;
import com.pestcontrol.backend.domain.enums.UserRole;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.service.ReservationEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendReservationConfirmation_whenEmailPresent_sendsMessageWithTicketDetailsAndFromAddress() {
        ReservationEmailService service = new ReservationEmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@pestcontrol.com");

        Reservation reservation = buildReservation(42L, "Mega Concert");
        Ticket ticket = buildTicket(99L, reservation, "59.99", TicketStatus.ISSUED);

        service.sendReservationConfirmation("customer@example.com", reservation, List.of(ticket));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals(1, sent.getTo().length);
        assertEquals("customer@example.com", sent.getTo()[0]);
        assertEquals("Reservation Confirmed", sent.getSubject());
        assertEquals("noreply@pestcontrol.com", sent.getFrom());

        String body = sent.getText();
        assertTrue(body != null && body.contains("Reservation ID: 42"));
        assertTrue(body.contains("Event: Mega Concert"));
        assertTrue(body.contains("Start: 2026-04-10 18:00 UTC"));
        assertTrue(body.contains("End: 2026-04-10 21:00 UTC"));
        assertTrue(body.contains("Location: Bell Centre, 1 Arena, Montreal, QC, H1A1A1"));
        assertTrue(body.contains("Ticket ID: 99"));
        assertTrue(body.contains("Price: 59.99"));
        assertTrue(body.contains("Status: ISSUED"));
    }

    @Test
    void sendCancellationConfirmation_whenEmailPresent_sendsMessageWithVoidedTicketDetails() {
        ReservationEmailService service = new ReservationEmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@pestcontrol.com");

        Reservation reservation = buildReservation(7L, "Comedy Night");
        Ticket ticket = buildTicket(501L, reservation, "39.99", TicketStatus.VOIDED);

        service.sendCancellationConfirmation("customer@example.com", reservation, List.of(ticket));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("Reservation Cancelled", sent.getSubject());

        String body = sent.getText();
        assertTrue(body != null && body.contains("Your reservation has been cancelled."));
        assertTrue(body.contains("Reservation ID: 7"));
        assertTrue(body.contains("Event: Comedy Night"));
        assertTrue(body.contains("Invalidated Ticket Information:"));
        assertTrue(body.contains("Ticket ID: 501"));
        assertTrue(body.contains("Status: VOIDED"));
    }

    @Test
    void sendReservationConfirmation_whenEmailIsNull_doesNotSend() {
        ReservationEmailService service = new ReservationEmailService(mailSender);

        service.sendReservationConfirmation(null, buildReservation(1L, "No Email Event"), List.of());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendCancellationConfirmation_whenEmailIsBlank_doesNotSend() {
        ReservationEmailService service = new ReservationEmailService(mailSender);

        service.sendCancellationConfirmation("   ", buildReservation(2L, "No Email Event"), List.of());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendReservationConfirmation_whenFromAddressBlank_omitsFromHeader() {
        ReservationEmailService service = new ReservationEmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", " ");

        Reservation reservation = buildReservation(3L, "No From Event");
        Ticket ticket = buildTicket(88L, reservation, "29.99", TicketStatus.ISSUED);

        service.sendReservationConfirmation("customer@example.com", reservation, List.of(ticket));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertNull(captor.getValue().getFrom());
    }

    @Test
    void sendEmail_whenMailSenderThrows_doesNotPropagateException() {
        ReservationEmailService service = new ReservationEmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@pestcontrol.com");

        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        Reservation reservation = buildReservation(9L, "Resiliency Event");
        Ticket ticket = buildTicket(901L, reservation, "49.99", TicketStatus.ISSUED);

        assertDoesNotThrow(
                () -> service.sendReservationConfirmation("customer@example.com", reservation, List.of(ticket)));
    }

    private Reservation buildReservation(Long reservationId, String eventTitle) {
        User user = new User("Customer", "customer@example.com", "5145550000", "hash", UserRole.CUSTOMER);
        user.setUserId(11L);

        Location location = new Location("Bell Centre", "1 Arena", "Montreal", "QC", "H1A1A1");

        Event event = new Event(
                location,
                eventTitle,
                OffsetDateTime.parse("2026-04-10T18:00:00Z"),
                OffsetDateTime.parse("2026-04-10T21:00:00Z"),
                Category.CONCERT,
                new BigDecimal("59.99"),
                EventStatus.SCHEDULED);
        event.setEventId(77L);

        Reservation reservation = new Reservation(
                user,
                event,
                OffsetDateTime.parse("2026-04-01T12:00:00Z"),
                ReservationStatus.CONFIRMED,
                new BigDecimal("59.99"));
        reservation.setReservationId(reservationId);
        return reservation;
    }

    private Ticket buildTicket(Long ticketId, Reservation reservation, String price, TicketStatus status) {
        Ticket ticket = new Ticket(reservation, new BigDecimal(price));
        ticket.setTicketId(ticketId);
        ticket.setStatus(status);
        return ticket;
    }
}
