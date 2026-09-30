package io.github.moriyaeldar.luach.events;

import java.util.UUID;

/**
 * A family member as other services see it.
 *
 * @param userSubject the login identity linked to this member (e.g. "google:1234"), or {@code null} for a member
 *                    without an account, such as a young child
 */
public record MemberSnapshot(UUID id, String displayName, String color, Role role, String userSubject) {
}
