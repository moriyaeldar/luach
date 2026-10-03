package io.github.moriyaeldar.luach.household.domain;

import io.github.moriyaeldar.luach.events.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A person in a family. Not everyone logs in: a young child can be a member without an account, and an invite link
 * can later connect a member to a login.
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Member() {
    }

    public Member(UUID householdId, String displayName, String color, Role role, UUID userId) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.displayName = displayName;
        this.color = color;
        this.role = role;
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    public void update(String displayName, String color, Role role) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (color != null) {
            this.color = color;
        }
        if (role != null) {
            this.role = role;
        }
    }

    public void linkUser(UUID userId) {
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public Role getRole() {
        return role;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
