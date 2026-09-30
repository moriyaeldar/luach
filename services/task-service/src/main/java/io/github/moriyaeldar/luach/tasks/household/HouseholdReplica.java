package io.github.moriyaeldar.luach.tasks.household;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** This service's copy of a family, built from Household service events. */
@Entity
@Table(name = "household_replica")
public class HouseholdReplica {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "in_israel", nullable = false)
    private boolean inIsrael;

    @Column(nullable = false)
    private boolean demo;

    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private List<MemberReplica> members = new ArrayList<>();

    protected HouseholdReplica() {
    }

    public HouseholdReplica(UUID id) {
        this.id = id;
    }

    /** Replaces the state with a newer snapshot, updating members in place so ids are never deleted and re-added. */
    void replace(String name, boolean inIsrael, boolean demo, long version, List<MemberReplica> newMembers) {
        this.name = name;
        this.inIsrael = inIsrael;
        this.demo = demo;
        this.version = version;
        this.updatedAt = Instant.now();
        var incoming = new java.util.LinkedHashMap<UUID, MemberReplica>();
        newMembers.forEach(m -> incoming.put(m.getId(), m));
        members.removeIf(m -> !incoming.containsKey(m.getId()));
        for (MemberReplica current : members) {
            current.copyFrom(incoming.remove(current.getId()));
        }
        members.addAll(incoming.values());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isInIsrael() {
        return inIsrael;
    }

    public boolean isDemo() {
        return demo;
    }

    public long getVersion() {
        return version;
    }

    public List<MemberReplica> getMembers() {
        return members;
    }
}
