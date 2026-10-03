package io.github.moriyaeldar.luach.household.service;

import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.events.MemberSnapshot;
import io.github.moriyaeldar.luach.household.domain.AppUser;
import io.github.moriyaeldar.luach.household.domain.AppUserRepository;
import io.github.moriyaeldar.luach.household.domain.Household;
import io.github.moriyaeldar.luach.household.domain.Member;
import io.github.moriyaeldar.luach.household.domain.MemberRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Builds the snapshot other services receive: the household and its members with their login subjects. */
@Component
public class SnapshotAssembler {

    private final MemberRepository members;
    private final AppUserRepository users;

    public SnapshotAssembler(MemberRepository members, AppUserRepository users) {
        this.members = members;
        this.users = users;
    }

    public HouseholdSnapshot snapshot(Household household) {
        List<Member> list = members.findByHouseholdIdOrderByCreatedAt(household.getId());
        List<UUID> userIds = list.stream().map(Member::getUserId).filter(Objects::nonNull).toList();
        Map<UUID, String> subjects = users.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getSubject));
        return new HouseholdSnapshot(household.getId(), household.getName(), household.isInIsrael(),
                household.isDemo(), household.getSnapshotVersion(), list.stream()
                .map(m -> new MemberSnapshot(m.getId(), m.getDisplayName(), m.getColor(), m.getRole(),
                        m.getUserId() == null ? null : subjects.get(m.getUserId())))
                .toList());
    }

}
