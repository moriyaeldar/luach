package io.github.moriyaeldar.luach.household.web;

import io.github.moriyaeldar.luach.household.api.Requests;
import io.github.moriyaeldar.luach.household.api.Views;
import io.github.moriyaeldar.luach.household.security.Identity;
import io.github.moriyaeldar.luach.household.service.HouseholdService;
import io.github.moriyaeldar.luach.household.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class HouseholdController {

    private final HouseholdService households;
    private final InviteService invites;

    public HouseholdController(HouseholdService households, InviteService invites) {
        this.households = households;
        this.invites = invites;
    }

    /** Who am I, and which families do I belong to. */
    @GetMapping("/api/me")
    public Views.Me me() {
        return households.me(Identity.current());
    }

    @PostMapping("/api/households")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.MembershipView create(@Valid @RequestBody Requests.CreateHousehold request) {
        return households.create(Identity.current(), request);
    }

    @PatchMapping("/api/households/{id}")
    public Views.MembershipView update(@PathVariable UUID id, @Valid @RequestBody Requests.UpdateHousehold request) {
        return households.update(Identity.current(), id, request);
    }

    @GetMapping("/api/households/{id}/members")
    public List<Views.MemberView> members(@PathVariable UUID id) {
        return households.members(Identity.current(), id);
    }

    @PostMapping("/api/households/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.MemberView addMember(@PathVariable UUID id, @Valid @RequestBody Requests.AddMember request) {
        return households.addMember(Identity.current(), id, request);
    }

    @PatchMapping("/api/households/{id}/members/{memberId}")
    public Views.MemberView updateMember(@PathVariable UUID id, @PathVariable UUID memberId,
                                         @Valid @RequestBody Requests.UpdateMember request) {
        return households.updateMember(Identity.current(), id, memberId, request);
    }

    @DeleteMapping("/api/households/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID memberId) {
        households.removeMember(Identity.current(), id, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/households/{id}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public Views.InviteCreated invite(@PathVariable UUID id, @Valid @RequestBody Requests.CreateInvite request) {
        return invites.create(Identity.current(), id, request);
    }

    /** Public: shows who invited you where, before you log in. */
    @GetMapping("/api/invites/{token}")
    public Views.InvitePreview preview(@PathVariable String token) {
        return invites.preview(token);
    }

    @PostMapping("/api/invites/{token}/accept")
    public Views.MembershipView accept(@PathVariable String token) {
        return invites.accept(Identity.current(), token);
    }
}
