package io.github.moriyaeldar.luach.events;

/** A family member's role. */
public enum Role {
    /** Manages the family: members, invites, settings. Full access to tasks. */
    ADMIN,
    /** Creates and edits tasks. */
    MEMBER,
    /** Sees the schedule and ticks off their own tasks. */
    CHILD
}
