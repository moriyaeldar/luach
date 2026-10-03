package io.github.moriyaeldar.luach.household.web;

import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.household.domain.HouseholdRepository;
import io.github.moriyaeldar.luach.household.service.Errors;
import io.github.moriyaeldar.luach.household.service.SnapshotAssembler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service API (tokens with the "internal" scope only; the Gateway never routes here). Lets a service
 * catch up on a household it hasn't heard about through Kafka yet.
 */
@RestController
public class InternalController {

    private final HouseholdRepository households;
    private final SnapshotAssembler snapshots;

    public InternalController(HouseholdRepository households, SnapshotAssembler snapshots) {
        this.households = households;
        this.snapshots = snapshots;
    }

    @GetMapping("/internal/households/{id}")
    @Transactional(readOnly = true)
    public HouseholdSnapshot snapshot(@PathVariable UUID id) {
        return snapshots.snapshot(households.findById(id).orElseThrow(() -> new Errors.NotFound("Household not found")));
    }
}
