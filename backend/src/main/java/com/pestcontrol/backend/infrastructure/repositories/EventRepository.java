package com.pestcontrol.backend.infrastructure.repositories;

import com.pestcontrol.backend.domain.Event;
import com.pestcontrol.backend.domain.Location;
import com.pestcontrol.backend.domain.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCategory(Category category);

    List<Event> findByLocation(Location location);

    List<Event> findByStartDateTimeAfter(OffsetDateTime time);

    List<Event> findByCategoryAndLocation(Category category, Location location);

    List<Event> findByTitleContainingIgnoreCase(String keyword);
}
