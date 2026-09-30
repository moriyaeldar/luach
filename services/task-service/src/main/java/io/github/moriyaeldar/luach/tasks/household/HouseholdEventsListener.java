package io.github.moriyaeldar.luach.tasks.household;

import io.github.moriyaeldar.luach.events.HouseholdEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumes household events. Processing is idempotent (each event id once, stale snapshots ignored), so redelivery
 * after a crash or a rebalance is harmless.
 */
@Component
public class HouseholdEventsListener {

    private static final Logger log = LoggerFactory.getLogger(HouseholdEventsListener.class);

    private final HouseholdReplicaRepository replicas;
    private final HouseholdDirectory directory;
    private final DemoSeeder demoSeeder;
    private final TransactionTemplate tx;
    private final JsonMapper json;

    public HouseholdEventsListener(HouseholdReplicaRepository replicas, HouseholdDirectory directory,
                                   DemoSeeder demoSeeder, TransactionTemplate tx, JsonMapper json) {
        this.replicas = replicas;
        this.directory = directory;
        this.demoSeeder = demoSeeder;
        this.tx = tx;
        this.json = json;
    }

    @KafkaListener(topics = HouseholdEvent.TOPIC, autoStartup = "${luach.kafka.enabled:true}")
    public void on(String message) {
        HouseholdEvent event = json.readValue(message, HouseholdEvent.class);
        tx.executeWithoutResult(status -> {
            if (replicas.markProcessed(event.id()) == 0) {
                return; // already handled
            }
            switch (event.type()) {
                case HOUSEHOLD_UPSERTED -> {
                    directory.apply(event.snapshot());
                    if (event.snapshot().demo()) {
                        demoSeeder.seedOnce(replicas.findById(event.householdId()).orElseThrow());
                    }
                }
                case HOUSEHOLD_DELETED -> directory.delete(event.householdId());
            }
        });
        log.debug("Processed {} for household {}", event.type(), event.householdId());
    }
}
