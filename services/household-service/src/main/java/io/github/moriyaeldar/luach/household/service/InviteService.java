package io.github.moriyaeldar.luach.household.service;

import io.github.moriyaeldar.luach.events.Role;
import io.github.moriyaeldar.luach.household.api.Requests;
import io.github.moriyaeldar.luach.household.api.Views;
import io.github.moriyaeldar.luach.household.domain.AppUser;
import io.github.moriyaeldar.luach.household.domain.Household;
import io.github.moriyaeldar.luach.household.domain.Invite;
import io.github.moriyaeldar.luach.household.domain.InviteRepository;
import io.github.moriyaeldar.luach.household.domain.Member;
import io.github.moriyaeldar.luach.household.domain.MemberRepository;
import io.github.moriyaeldar.luach.household.outbox.HouseholdEventRecorder;
import io.github.moriyaeldar.luach.household.security.Identity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class InviteService {

    private final InviteRepository invites;
    private final MemberRepository members;
    private final HouseholdService households;
    private final HouseholdEventRecorder events;

    public InviteService(InviteRepository invites, MemberRepository members, HouseholdService households,
                         HouseholdEventRecorder events) {
        this.invites = invites;
        this.members = members;
        this.households = households;
        this.events = events;
    }

    public Views.InviteCreated create(Identity identity, UUID householdId, Requests.CreateInvite request) {
        Member me = households.requireRole(identity, householdId, Role.ADMIN);
        Invite invite;
        if (request.memberId() != null) {
            Member target = households.member(householdId, request.memberId());
            if (target.getUserId() != null) {
                throw new Errors.Invalid(target.getDisplayName() + " is already connected");
            }
            invite = new Invite(householdId, target.getId(), null, target.getRole(), me.getId());
        } else {
            if (request.displayName() == null || request.displayName().isBlank()) {
                throw new Errors.Invalid("Give a name, or choose an existing member to invite");
            }
            invite = new Invite(householdId, null, request.displayName().strip(),
                    request.role() == null ? Role.MEMBER : request.role(), me.getId());
        }
        invites.save(invite);
        return new Views.InviteCreated(invite.getToken(), invite.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public Views.InvitePreview preview(String token) {
        Invite invite = find(token);
        Household household = households.household(invite.getHouseholdId());
        String name = invite.getMemberId() != null
                ? members.findById(invite.getMemberId()).map(Member::getDisplayName).orElse(null)
                : invite.getDisplayName();
        String status = invite.getAcceptedAt() != null ? "USED"
                : Instant.now().isAfter(invite.getExpiresAt()) ? "EXPIRED" : "VALID";
        return new Views.InvitePreview(household.getName(), name, invite.getRole(), status);
    }

    /** Joins the invited family. Accepting twice, or accepting while already a member, is harmless. */
    public Views.MembershipView accept(Identity identity, String token) {
        Invite invite = find(token);
        AppUser user = households.currentUser(identity);
        Household household = households.household(invite.getHouseholdId());

        var existing = members.findByHouseholdIdAndUserId(household.getId(), user.getId());
        if (existing.isPresent()) {
            Member m = existing.get();
            return new Views.MembershipView(household.getId(), household.getName(), household.isInIsrael(),
                    household.isDemo(), m.getId(), m.getRole());
        }
        if (!invite.isUsable(Instant.now())) {
            throw new Errors.Gone("This invite link was already used or has expired. Ask for a new one.");
        }

        Member member;
        if (invite.getMemberId() != null) {
            member = members.findById(invite.getMemberId())
                    .orElseThrow(() -> new Errors.Gone("The invited member no longer exists"));
            if (member.getUserId() != null) {
                throw new Errors.Gone("This member is already connected to another account");
            }
            member.linkUser(user.getId());
        } else {
            member = members.save(new Member(household.getId(), invite.getDisplayName(),
                    households.nextColor(household.getId()), invite.getRole(), user.getId()));
        }
        invite.accept(user.getId());
        events.upserted(household);
        return new Views.MembershipView(household.getId(), household.getName(), household.isInIsrael(),
                household.isDemo(), member.getId(), member.getRole());
    }

    private Invite find(String token) {
        return invites.findById(token).orElseThrow(() -> new Errors.NotFound("Invite not found"));
    }
}
