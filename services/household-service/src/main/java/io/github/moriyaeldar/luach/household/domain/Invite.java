package io.github.moriyaeldar.luach.household.domain;

import io.github.moriyaeldar.luach.events.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * A single-use link that lets a person join a family. It either connects them to an existing member (e.g. "Dad", added
 * before he had an account) or creates a new member.
 */
@Entity
@Table(name = "invite")
public class Invite {

    public static final Duration VALIDITY = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    private String token;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_by_member", nullable = false)
    private UUID createdByMember;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by_user")
    private UUID acceptedByUser;

    protected Invite() {
    }

    public Invite(UUID householdId, UUID memberId, String displayName, Role role, UUID createdByMember) {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        this.token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        this.householdId = householdId;
        this.memberId = memberId;
        this.displayName = displayName;
        this.role = role;
        this.createdByMember = createdByMember;
        this.expiresAt = Instant.now().plus(VALIDITY);
    }

    public boolean isUsable(Instant now) {
        return acceptedAt == null && now.isBefore(expiresAt);
    }

    public void accept(UUID userId) {
        this.acceptedAt = Instant.now();
        this.acceptedByUser = userId;
    }

    public String getToken() {
        return token;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
