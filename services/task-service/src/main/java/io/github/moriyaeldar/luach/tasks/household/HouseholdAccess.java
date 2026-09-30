package io.github.moriyaeldar.luach.tasks.household;

import io.github.moriyaeldar.luach.events.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Checks that the current user belongs to a family and may do what they're asking. Uses the local copy first and
 * asks the Household service only when the copy doesn't know the family or the user yet.
 */
@Component
public class HouseholdAccess {

    private final HouseholdDirectory directory;
    private final DemoSeeder demoSeeder;

    public HouseholdAccess(HouseholdDirectory directory, DemoSeeder demoSeeder) {
        this.directory = directory;
        this.demoSeeder = demoSeeder;
    }

    public enum Permission {
        /** See the schedule. Everyone in the family. */
        READ,
        /** Create, edit and delete tasks. Admins and members, not children. */
        EDIT
    }

    /** The family and the current user's member in it. */
    public record Access(HouseholdReplica household, MemberReplica member) {
        public boolean isChild() {
            return member.getRole() == Role.CHILD;
        }
    }

    public Access require(UUID householdId, Permission permission) {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        Access access = lookup(directory.find(householdId), subject)
                .or(() -> lookup(directory.refresh(householdId), subject))
                .orElseThrow(() -> new ForbiddenException("You're not a member of this family"));
        if (permission == Permission.EDIT && access.isChild()) {
            throw new ForbiddenException("Children can tick off their tasks but not edit the schedule");
        }
        if (access.household().isDemo()) {
            demoSeeder.seedOnce(access.household());
        }
        return access;
    }

    private static Optional<Access> lookup(Optional<HouseholdReplica> household, String subject) {
        return household.flatMap(h -> h.getMembers().stream()
                .filter(m -> subject.equals(m.getUserSubject()))
                .findFirst()
                .map(m -> new Access(h, m)));
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
