package io.github.moriyaeldar.luach.household.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InviteRepository extends JpaRepository<Invite, String> {

    void deleteByHouseholdId(UUID householdId);
}
