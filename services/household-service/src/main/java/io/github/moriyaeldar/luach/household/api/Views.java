package io.github.moriyaeldar.luach.household.api;

import io.github.moriyaeldar.luach.events.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response bodies. */
public final class Views {

    private Views() {
    }

    public record Me(UserView user, List<MembershipView> households) {
    }

    public record UserView(UUID id, String name, String email, String picture, boolean demo) {
    }

    /** A household the current user belongs to, with their own member id and role in it. */
    public record MembershipView(UUID id, String name, boolean inIsrael, boolean demo, UUID memberId, Role role) {
    }

    public record MemberView(UUID id, String displayName, String color, Role role, boolean connected, String email) {
    }

    public record InviteCreated(String token, Instant expiresAt) {
    }

    public record InvitePreview(String householdName, String displayName, Role role, String status) {
    }
}
