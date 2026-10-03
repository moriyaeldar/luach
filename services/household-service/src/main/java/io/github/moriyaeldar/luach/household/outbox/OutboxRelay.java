package io.github.moriyaeldar.luach.household.outbox;

import io.github.moriyaeldar.luach.events.HouseholdEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Relays outbox rows to Kafka in the order they were written. If Kafka is unreachable the rows stay in the table and
 * are sent on a later run, so no event is lost and the API keeps working while Kafka is down.
 */
@Component
@ConditionalOnProperty(name = "luach.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;
    private volatile boolean kafkaHealthy = true;

    public OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka, TransactionTemplate tx) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "${luach.outbox.poll-ms:1000}")
    public void relay() {
        for (OutboxEvent event : outbox.findByPublishedAtIsNullOrderByCreatedAt(Limit.of(100))) {
            try {
                kafka.send(HouseholdEvent.TOPIC, event.getHouseholdId().toString(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (kafkaHealthy) {
                    log.warn("Kafka unavailable, keeping {} outbox event(s) for later: {}",
                            outbox.countByPublishedAtIsNull(), e.getMessage());
                }
                kafkaHealthy = false;
                return; // keep order: don't skip ahead of a failed event
            }
            tx.executeWithoutResult(s -> outbox.findById(event.getId()).ifPresent(OutboxEvent::markPublished));
            if (!kafkaHealthy) {
                log.info("Kafka is back, relaying queued events");
                kafkaHealthy = true;
            }
        }
    }

    @Configuration
    static class Topics {
        @Bean
        NewTopic householdEventsTopic() {
            return TopicBuilder.name(HouseholdEvent.TOPIC).partitions(2).build();
        }
    }
}
