package com.pestcontrol.backend.infrastructure.repositories;

import com.pestcontrol.backend.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}