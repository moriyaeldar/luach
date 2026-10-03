package io.github.moriyaeldar.luach.household.outbox;

import io.github.moriyaeldar.luach.events.HouseholdEvent;
import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.household.domain.Household;
import io.github.moriyaeldar.luach.household.service.SnapshotAssembler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes household events to the outbox table. Must run inside the transaction that made the change, so the change and
 * its event are committed together or not at all.
 */
@Component
public class HouseholdEventRecorder {

    private final OutboxRepository outbox;
    private final SnapshotAssembler snapshots;
    private final JsonMapper json;

    public HouseholdEventRecorder(OutboxRepository outbox, SnapshotAssembler snapshots, JsonMapper json) {
        this.outbox = outbox;
        this.snapshots = snapshots;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void upserted(Household household) {
        household.nextSnapshotVersion();
        HouseholdSnapshot snapshot = snapshots.snapshot(household);
        record(new HouseholdEvent(UUID.randomUUID(), HouseholdEvent.Type.HOUSEHOLD_UPSERTED, Instant.now(),
                household.getId(), snapshot));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleted(UUID householdId) {
        record(new HouseholdEvent(UUID.randomUUID(), HouseholdEvent.Type.HOUSEHOLD_DELETED, Instant.now(),
                householdId, null));
    }

    private void record(HouseholdEvent event) {
        outbox.save(new OutboxEvent(event.id(), event.householdId(), event.type().name(),
                json.writeValueAsString(event), event.occurredAt()));
    }
}
