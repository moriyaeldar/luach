package io.github.moriyaeldar.luach.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@link #TOPIC}, keyed by household id so all events of one household keep their order.
 *
 * @param id       unique per event; consumers use it to process each event once
 * @param snapshot the household after the change; {@code null} for {@link Type#HOUSEHOLD_DELETED}
 */
public record HouseholdEvent(UUID id, Type type, Instant occurredAt, UUID householdId, HouseholdSnapshot snapshot) {

    public static final String TOPIC = "household-events";

    public enum Type {
        HOUSEHOLD_UPSERTED,
        HOUSEHOLD_DELETED
    }
}
