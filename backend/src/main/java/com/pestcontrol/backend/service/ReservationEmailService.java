package com.pestcontrol.backend.service;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.Ticket;
import com.pestcontrol.backend.domain.enums.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReservationEmailService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationEmailService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final JavaMailSender mailSender;

    @Value("${MAIL_FROM:${spring.mail.username:}}")
    private String fromAddress;

    public ReservationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReservationConfirmation(String email, Reservation reservation, List<Ticket> tickets) {
        if (email == null || email.isBlank()) {
            return;
        }

        String body = buildReservationConfirmationBody(reservation, tickets);
        sendEmail(email, "Reservation Confirmed", body);
    }

    public void sendCancellationConfirmation(String email, Reservation reservation, List<Ticket> tickets) {
        if (email == null || email.isBlank()) {
            return;
        }

        String body = buildCancellationConfirmationBody(reservation, tickets);
        sendEmail(email, "Reservation Cancelled", body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception exception) {
            logger.error("Failed to send reservation email to {}", to, exception);
        }
    }

    private String buildReservationConfirmationBody(Reservation reservation, List<Ticket> tickets) {
        StringBuilder builder = new StringBuilder();
        Event event = reservation.getEvent();
        Location location = event.getLocation();

        builder.append("Your reservation has been confirmed.\n\n")
                .append("Reservation ID: ").append(reservation.getReservationId()).append("\n")
                .append("Event: ").append(event.getTitle()).append("\n")
                .append("Start: ").append(event.getStartDateTime().format(DATE_TIME_FORMATTER)).append("\n")
                .append("End: ").append(event.getEndDateTime().format(DATE_TIME_FORMATTER)).append("\n")
                .append("Location: ").append(location.getName())
                .append(", ").append(location.getAddressLine())
                .append(", ").append(location.getCity())
                .append(", ").append(location.getProvince())
                .append(", ").append(location.getPostalCode())
                .append("\n\n")
                .append("Ticket Information:\n");

        for (Ticket ticket : tickets) {
            builder.append("- Ticket ID: ").append(ticket.getTicketId())
                    .append(" | Price: ").append(ticket.getPrice())
                    .append(" | Status: ").append(ticket.getStatus())
                    .append("\n");
        }

        return builder.toString();
    }

    private String buildCancellationConfirmationBody(Reservation reservation, List<Ticket> tickets) {
        StringBuilder builder = new StringBuilder();
        Event event = reservation.getEvent();

        builder.append("Your reservation has been cancelled.\n\n")
                .append("Reservation ID: ").append(reservation.getReservationId()).append("\n")
                .append("Event: ").append(event.getTitle()).append("\n")
                .append("\nInvalidated Ticket Information:\n");

        for (Ticket ticket : tickets) {
            TicketStatus status = ticket.getStatus();
            builder.append("- Ticket ID: ").append(ticket.getTicketId())
                    .append(" | Price: ").append(ticket.getPrice())
                    .append(" | Status: ").append(status)
                    .append("\n");
        }

        return builder.toString();
    }
}
