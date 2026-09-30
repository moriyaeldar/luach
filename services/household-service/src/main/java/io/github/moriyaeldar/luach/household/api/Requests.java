package io.github.moriyaeldar.luach.household.api;

import io.github.moriyaeldar.luach.events.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request bodies. */
public final class Requests {

    private Requests() {
    }

    public record CreateHousehold(@NotBlank @Size(max = 100) String name, Boolean inIsrael) {
    }

    public record UpdateHousehold(@NotBlank @Size(max = 100) String name, Boolean inIsrael) {
    }

    public record AddMember(@NotBlank @Size(max = 60) String displayName, Role role,
                            @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color) {
    }

    public record UpdateMember(@Size(min = 1, max = 60) String displayName, Role role,
                               @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color) {
    }

    /** Either for an existing member ({@code memberId}) or for a new one ({@code displayName} and {@code role}). */
    public record CreateInvite(UUID memberId, @Size(max = 60) String displayName, Role role) {
    }
}
