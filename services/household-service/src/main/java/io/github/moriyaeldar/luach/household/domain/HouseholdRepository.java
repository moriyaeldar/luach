package io.github.moriyaeldar.luach.household.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {

    List<Household> findByDemoTrueAndCreatedAtBefore(Instant cutoff);
}
