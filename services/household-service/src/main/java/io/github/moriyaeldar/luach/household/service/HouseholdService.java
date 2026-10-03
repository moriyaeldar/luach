package io.github.moriyaeldar.luach.household.service;

import io.github.moriyaeldar.luach.events.Role;
import io.github.moriyaeldar.luach.household.api.Requests;
import io.github.moriyaeldar.luach.household.api.Views;
import io.github.moriyaeldar.luach.household.domain.AppUser;
import io.github.moriyaeldar.luach.household.domain.AppUserRepository;
import io.github.moriyaeldar.luach.household.domain.Household;
import io.github.moriyaeldar.luach.household.domain.HouseholdRepository;
import io.github.moriyaeldar.luach.household.domain.Member;
import io.github.moriyaeldar.luach.household.domain.MemberRepository;
import io.github.moriyaeldar.luach.household.outbox.HouseholdEventRecorder;
import io.github.moriyaeldar.luach.household.security.Identity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class HouseholdService {

    static final List<String> PALETTE = List.of(
            "#7c3aed", "#0891b2", "#db2777", "#ea580c", "#16a34a", "#ca8a04", "#4f46e5", "#dc2626");

    private final AppUserRepository users;
    private final HouseholdRepository households;
    private final MemberRepository members;
    private final HouseholdEventRecorder events;
    private final DemoHouseholdFactory demoFactory;

    public HouseholdService(AppUserRepository users, HouseholdRepository households, MemberRepository members,
                            HouseholdEventRecorder events, DemoHouseholdFactory demoFactory) {
        this.users = users;
        this.households = households;
        this.members = members;
        this.events = events;
        this.demoFactory = demoFactory;
    }

    /** The current user and their households. Creates the user on first login, and a sample family for demo users. */
    public Views.Me me(Identity identity) {
        AppUser user = currentUser(identity);
        if (identity.demo() && members.findByUserId(user.getId()).isEmpty()) {
            demoFactory.create(user);
        }
        List<Views.MembershipView> memberships = members.findByUserId(user.getId()).stream()
                .map(m -> {
                    Household h = households.findById(m.getHouseholdId()).orElseThrow();
                    return new Views.MembershipView(h.getId(), h.getName(), h.isInIsrael(), h.isDemo(), m.getId(),
                            m.getRole());
                })
                .toList();
        return new Views.Me(new Views.UserView(user.getId(), user.getName(), user.getEmail(), user.getPictureUrl(),
                identity.demo()), memberships);
    }

    public Views.MembershipView create(Identity identity, Requests.CreateHousehold request) {
        AppUser user = currentUser(identity);
        Household household = households.save(new Household(request.name().strip(),
                request.inIsrael() == null || request.inIsrael(), false));
        String name = user.getName() == null || user.getName().isBlank() ? "Me" : user.getName();
        Member me = members.save(new Member(household.getId(), truncate(name), PALETTE.getFirst(), Role.ADMIN,
                user.getId()));
        events.upserted(household);
        return new Views.MembershipView(household.getId(), household.getName(), household.isInIsrael(), false,
                me.getId(), Role.ADMIN);
    }

    public Views.MembershipView update(Identity identity, UUID householdId, Requests.UpdateHousehold request) {
        Member me = requireRole(identity, householdId, Role.ADMIN);
        Household household = household(householdId);
        household.update(request.name().strip(), request.inIsrael() == null ? household.isInIsrael() : request.inIsrael());
        events.upserted(household);
        return new Views.MembershipView(household.getId(), household.getName(), household.isInIsrael(),
                household.isDemo(), me.getId(), me.getRole());
    }

    @Transactional(readOnly = true)
    public List<Views.MemberView> members(Identity identity, UUID householdId) {
        requireMember(identity, householdId);
        List<Member> list = members.findByHouseholdIdOrderByCreatedAt(householdId);
        Map<UUID, AppUser> byId = users.findByIdIn(list.stream().map(Member::getUserId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return list.stream().map(m -> view(m, m.getUserId() == null ? null : byId.get(m.getUserId()))).toList();
    }

    public Views.MemberView addMember(Identity identity, UUID householdId, Requests.AddMember request) {
        requireRole(identity, householdId, Role.ADMIN);
        Member member = members.save(new Member(householdId, request.displayName().strip(),
                request.color() != null ? request.color() : nextColor(householdId),
                request.role() == null ? Role.MEMBER : request.role(), null));
        events.upserted(household(householdId));
        return view(member, null);
    }

    public Views.MemberView updateMember(Identity identity, UUID householdId, UUID memberId,
                                         Requests.UpdateMember request) {
        requireRole(identity, householdId, Role.ADMIN);
        Member member = member(householdId, memberId);
        if (member.getRole() == Role.ADMIN && request.role() != null && request.role() != Role.ADMIN) {
            requireAnotherAdmin(householdId, memberId);
        }
        member.update(request.displayName() == null ? null : request.displayName().strip(), request.color(),
                request.role());
        events.upserted(household(householdId));
        return view(member, member.getUserId() == null ? null : users.findById(member.getUserId()).orElse(null));
    }

    public void removeMember(Identity identity, UUID householdId, UUID memberId) {
        requireRole(identity, householdId, Role.ADMIN);
        Member member = member(householdId, memberId);
        if (member.getRole() == Role.ADMIN) {
            requireAnotherAdmin(householdId, memberId);
        }
        members.delete(member);
        events.upserted(household(householdId));
    }

    // --- access helpers, also used by the invite service -----------------------------------------------------

    AppUser currentUser(Identity identity) {
        AppUser user = users.findBySubject(identity.subject()).orElseGet(() -> users.save(new AppUser(identity.subject())));
        user.updateProfile(identity.name(), identity.email(), identity.picture());
        return user;
    }

    Member requireMember(Identity identity, UUID householdId) {
        AppUser user = users.findBySubject(identity.subject())
                .orElseThrow(() -> new Errors.Forbidden("Not a member of this household"));
        return members.findByHouseholdIdAndUserId(householdId, user.getId())
                .orElseThrow(() -> new Errors.Forbidden("Not a member of this household"));
    }

    Member requireRole(Identity identity, UUID householdId, Role role) {
        Member me = requireMember(identity, householdId);
        if (me.getRole() != role) {
            throw new Errors.Forbidden("Only a family admin can do this");
        }
        return me;
    }

    Household household(UUID id) {
        return households.findById(id).orElseThrow(() -> new Errors.NotFound("Household not found"));
    }

    Member member(UUID householdId, UUID memberId) {
        return members.findByIdAndHouseholdId(memberId, householdId)
                .orElseThrow(() -> new Errors.NotFound("Member not found"));
    }

    String nextColor(UUID householdId) {
        Set<String> used = new HashSet<>();
        members.findByHouseholdIdOrderByCreatedAt(householdId).forEach(m -> used.add(m.getColor()));
        return PALETTE.stream().filter(c -> !used.contains(c)).findFirst()
                .orElse(PALETTE.get(used.size() % PALETTE.size()));
    }

    private void requireAnotherAdmin(UUID householdId, UUID memberId) {
        boolean another = members.findByHouseholdIdOrderByCreatedAt(householdId).stream()
                .anyMatch(m -> m.getRole() == Role.ADMIN && !m.getId().equals(memberId));
        if (!another) {
            throw new Errors.Invalid("A family needs at least one admin");
        }
    }

    static Views.MemberView view(Member m, AppUser user) {
        return new Views.MemberView(m.getId(), m.getDisplayName(), m.getColor(), m.getRole(), user != null,
                user == null ? null : user.getEmail());
    }

    private static String truncate(String s) {
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
