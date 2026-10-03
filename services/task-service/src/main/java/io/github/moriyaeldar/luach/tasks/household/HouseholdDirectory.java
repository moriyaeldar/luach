package io.github.moriyaeldar.luach.tasks.household;

import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.tasks.domain.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * Where this service learns about families. Kafka events keep the local copy up to date; if a family is missing or
 * stale (Kafka down, or an event still on its way), it is fetched from the Household service directly.
 */
@Service
public class HouseholdDirectory {

    private static final Logger log = LoggerFactory.getLogger(HouseholdDirectory.class);

    private final HouseholdReplicaRepository replicas;
    private final TaskRepository tasks;
    private final HouseholdClient client;
    /** Fetched families are saved in their own transaction: callers are often read-only (e.g. the week view). */
    private final TransactionTemplate newTransaction;

    public HouseholdDirectory(HouseholdReplicaRepository replicas, TaskRepository tasks, HouseholdClient client,
                              PlatformTransactionManager transactions) {
        this.replicas = replicas;
        this.tasks = tasks;
        this.client = client;
        this.newTransaction = new TransactionTemplate(transactions);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public Optional<HouseholdReplica> find(UUID id) {
        return replicas.findById(id);
    }

    /** Applies a snapshot unless we already have the same or a newer version. Returns whether it changed anything. */
    @Transactional
    public boolean apply(HouseholdSnapshot snapshot) {
        var existing = replicas.findById(snapshot.id());
        if (existing.isPresent() && existing.get().getVersion() >= snapshot.version()) {
            return false;
        }
        HouseholdReplica replica = existing.orElseGet(() -> new HouseholdReplica(snapshot.id()));
        replica.replace(snapshot.name(), snapshot.inIsrael(), snapshot.demo(), snapshot.version(),
                snapshot.members().stream()
                        .map(m -> new MemberReplica(m.id(), m.displayName(), m.color(), m.role(), m.userSubject()))
                        .toList());
        replicas.save(replica);
        return true;
    }

    /** The family was deleted in the Household service: drop its tasks and our copy. */
    @Transactional
    public void delete(UUID householdId) {
        tasks.deleteByHouseholdId(householdId);
        replicas.deleteDemoSeed(householdId);
        replicas.findById(householdId).ifPresent(replicas::delete);
    }

    /** Fetches the current state from the Household service. Empty if it doesn't exist or can't be reached. */
    public Optional<HouseholdReplica> refresh(UUID householdId) {
        try {
            return client.fetch(householdId).map(snapshot -> newTransaction.execute(status -> {
                apply(snapshot);
                HouseholdReplica replica = replicas.findById(householdId).orElseThrow();
                replica.getMembers().size(); // load before the transaction ends
                return replica;
            }));
        } catch (RuntimeException e) {
            log.warn("Couldn't fetch household {} from the Household service: {}", householdId, e.getMessage());
            return Optional.empty();
        }
    }
}
