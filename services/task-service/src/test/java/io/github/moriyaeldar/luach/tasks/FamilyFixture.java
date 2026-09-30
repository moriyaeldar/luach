package io.github.moriyaeldar.luach.tasks;

import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.events.MemberSnapshot;
import io.github.moriyaeldar.luach.events.Role;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** A test family: a parent (admin), a partner (member), a child with an account and a toddler without one. */
public final class FamilyFixture {

    public static final UUID HOUSEHOLD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PARENT = UUID.fromString("22222222-2222-2222-2222-222222222221");
    public static final UUID PARTNER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID CHILD = UUID.fromString("22222222-2222-2222-2222-222222222223");
    public static final UUID TODDLER = UUID.fromString("22222222-2222-2222-2222-222222222224");

    private FamilyFixture() {
    }

    public static HouseholdSnapshot snapshot(long version, boolean demo) {
        return new HouseholdSnapshot(HOUSEHOLD, "Family", true, demo, version, List.of(
                new MemberSnapshot(PARENT, "אמא", "#7c3aed", Role.ADMIN, "google:parent"),
                new MemberSnapshot(PARTNER, "אבא", "#0891b2", Role.MEMBER, "google:partner"),
                new MemberSnapshot(CHILD, "נועה", "#db2777", Role.CHILD, "google:child"),
                new MemberSnapshot(TODDLER, "איתי", "#ea580c", Role.CHILD, null)));
    }

    public static RequestPostProcessor as(String subject) {
        return jwt().jwt(j -> j.subject(subject).claim("name", subject));
    }

    public static RequestPostProcessor parent() {
        return as("google:parent");
    }
}
