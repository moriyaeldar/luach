package io.github.moriyaeldar.luach.household.service;

import io.github.moriyaeldar.luach.household.domain.Household;
import io.github.moriyaeldar.luach.household.domain.HouseholdRepository;
import io.github.moriyaeldar.luach.household.domain.InviteRepository;
import io.github.moriyaeldar.luach.household.domain.MemberRepository;
import io.github.moriyaeldar.luach.household.outbox.HouseholdEventRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Deletes demo families after their time is up. The deletion event tells other services to drop their data too. */
@Component
public class DemoCleanup {

    private static final Logger log = LoggerFactory.getLogger(DemoCleanup.class);

    private final HouseholdRepository households;
    private final MemberRepository members;
    private final InviteRepository invites;
    private final HouseholdEventRecorder events;
    private final Duration ttl;

    public DemoCleanup(HouseholdRepository households, MemberRepository members, InviteRepository invites,
                       HouseholdEventRecorder events, @Value("${luach.demo.ttl:24h}") Duration ttl) {
        this.households = households;
        this.members = members;
        this.invites = invites;
        this.events = events;
        this.ttl = ttl;
    }

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT1M")
    @Transactional
    public int deleteExpired() {
        var expired = households.findByDemoTrueAndCreatedAtBefore(Instant.now().minus(ttl));
        for (Household household : expired) {
            invites.deleteByHouseholdId(household.getId());
            members.deleteByHouseholdId(household.getId());
            households.delete(household);
            events.deleted(household.getId());
        }
        if (!expired.isEmpty()) {
            log.info("Deleted {} expired demo household(s)", expired.size());
        }
        return expired.size();
    }
}
