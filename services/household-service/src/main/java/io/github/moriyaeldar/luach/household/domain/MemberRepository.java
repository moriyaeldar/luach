package io.github.moriyaeldar.luach.household.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findByHouseholdIdOrderByCreatedAt(UUID householdId);

    List<Member> findByUserId(UUID userId);

    Optional<Member> findByHouseholdIdAndUserId(UUID householdId, UUID userId);

    Optional<Member> findByIdAndHouseholdId(UUID id, UUID householdId);

    void deleteByHouseholdId(UUID householdId);
}
