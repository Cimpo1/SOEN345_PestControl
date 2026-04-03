package com.pestcontrol.backend.infrastructure.repositories;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Reservation;
import com.pestcontrol.backend.domain.User;
import com.pestcontrol.backend.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUser(User user);

    List<Reservation> findByUserOrderByCreationDateDesc(User user);

    List<Reservation> findByEvent(Event event);

    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    List<Reservation> findByUserAndStatusOrderByCreationDateDesc(User user, ReservationStatus status);

    List<Reservation> findByUserAndEventAndStatusIn(User user, Event event, List<ReservationStatus> statuses);

    long countByEvent(Event event);
}
