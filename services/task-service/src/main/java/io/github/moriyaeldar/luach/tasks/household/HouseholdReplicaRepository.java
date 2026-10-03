package io.github.moriyaeldar.luach.tasks.household;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface HouseholdReplicaRepository extends JpaRepository<HouseholdReplica, UUID> {

    /** Returns 1 the first time an event id is seen, 0 afterwards. */
    @Modifying
    @Query(value = "insert into processed_event (id) values (:id) on conflict do nothing", nativeQuery = true)
    int markProcessed(UUID id);

    /** Returns 1 the first time a demo household is seeded, 0 afterwards. */
    @Modifying
    @Query(value = "insert into demo_seed (household_id) values (:id) on conflict do nothing", nativeQuery = true)
    int markDemoSeeded(UUID id);

    @Modifying
    @Query(value = "delete from demo_seed where household_id = :id", nativeQuery = true)
    void deleteDemoSeed(UUID id);
}
