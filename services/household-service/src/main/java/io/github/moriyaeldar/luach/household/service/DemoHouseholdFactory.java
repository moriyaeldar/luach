package io.github.moriyaeldar.luach.household.service;

import io.github.moriyaeldar.luach.events.Role;
import io.github.moriyaeldar.luach.household.domain.AppUser;
import io.github.moriyaeldar.luach.household.domain.Household;
import io.github.moriyaeldar.luach.household.domain.HouseholdRepository;
import io.github.moriyaeldar.luach.household.domain.Member;
import io.github.moriyaeldar.luach.household.domain.MemberRepository;
import io.github.moriyaeldar.luach.household.outbox.HouseholdEventRecorder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every demo visitor gets a private sample family: the visitor is the parent and admin, plus a partner and two children.
 * The Task service fills it with a sample week when it sees the household (from the Kafka event or on first access).
 * Demo households are deleted after a day by {@link DemoCleanup}.
 */
@Component
public class DemoHouseholdFactory {

    private final HouseholdRepository households;
    private final MemberRepository members;
    private final HouseholdEventRecorder events;

    public DemoHouseholdFactory(HouseholdRepository households, MemberRepository members,
                                HouseholdEventRecorder events) {
        this.households = households;
        this.members = members;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Household create(AppUser visitor) {
        Household household = households.save(new Household("משפחת דמו", true, true));
        members.save(new Member(household.getId(), "אמא", HouseholdService.PALETTE.get(0), Role.ADMIN, visitor.getId()));
        members.save(new Member(household.getId(), "אבא", HouseholdService.PALETTE.get(1), Role.MEMBER, null));
        members.save(new Member(household.getId(), "נועה", HouseholdService.PALETTE.get(2), Role.CHILD, null));
        members.save(new Member(household.getId(), "איתי", HouseholdService.PALETTE.get(3), Role.CHILD, null));
        events.upserted(household);
        return household;
    }
}
