package io.github.moriyaeldar.luach.events;

import java.util.List;
import java.util.UUID;

/**
 * The full state of a household. Events carry the whole snapshot (event-carried state transfer), so a consumer can
 * replace its copy instead of replaying deltas. {@code version} grows with every change; consumers ignore snapshots
 * older than the one they have.
 */
public record HouseholdSnapshot(UUID id, String name, boolean inIsrael, boolean demo, long version,
                                List<MemberSnapshot> members) {

    public HouseholdSnapshot {
        members = members == null ? List.of() : List.copyOf(members);
    }
}
