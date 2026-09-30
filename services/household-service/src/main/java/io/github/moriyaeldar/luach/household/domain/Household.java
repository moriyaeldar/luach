package io.github.moriyaeldar.luach.household.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household")
public class Household {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "in_israel", nullable = false)
    private boolean inIsrael;

    @Column(nullable = false)
    private boolean demo;

    /** Grows with every change that is published, so consumers can drop stale snapshots. */
    @Column(name = "snapshot_version", nullable = false)
    private long snapshotVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Household() {
    }

    public Household(String name, boolean inIsrael, boolean demo) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.inIsrael = inIsrael;
        this.demo = demo;
        this.createdAt = Instant.now();
    }

    public void update(String name, boolean inIsrael) {
        this.name = name;
        this.inIsrael = inIsrael;
    }

    public long nextSnapshotVersion() {
        return ++snapshotVersion;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
