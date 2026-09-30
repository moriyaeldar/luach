package io.github.moriyaeldar.luach.household.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findBySubject(String subject);

    List<AppUser> findByIdIn(Collection<UUID> ids);
}
