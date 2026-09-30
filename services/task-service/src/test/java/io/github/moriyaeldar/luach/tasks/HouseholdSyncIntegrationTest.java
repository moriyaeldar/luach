package io.github.moriyaeldar.luach.tasks;

import com.sun.net.httpserver.HttpServer;
import io.github.moriyaeldar.luach.events.HouseholdEvent;
import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.tasks.household.HouseholdDirectory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static io.github.moriyaeldar.luach.tasks.FamilyFixture.HOUSEHOLD;
import static io.github.moriyaeldar.luach.tasks.FamilyFixture.parent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** How the Task service learns about families: Kafka events, and a direct call when an event hasn't arrived. */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
@EmbeddedKafka(partitions = 2, topics = HouseholdEvent.TOPIC)
class HouseholdSyncIntegrationTest extends PostgresIntegrationTest {

    /** What the fake Household service returns from its internal API. */
    private static final AtomicReference<String> householdServiceBody = new AtomicReference<>();
    private static final HttpServer householdService = fakeHouseholdService();

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private HouseholdDirectory directory;
    @Autowired
    private JsonMapper json;

    private MockMvc mvc;

    @DynamicPropertySource
    static void householdServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("luach.household-service-url",
                () -> "http://localhost:" + householdService.getAddress().getPort());
    }

    @AfterAll
    static void stop() {
        householdService.stop(0);
    }

    @BeforeEach
    void setUp() {
        jdbc.execute("truncate task, task_completion, household_replica, member_replica, processed_event, demo_seed cascade");
        householdServiceBody.set(null);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private void publish(HouseholdEvent event) {
        kafka.send(HouseholdEvent.TOPIC, event.householdId().toString(), json.writeValueAsString(event));
    }

    private static HouseholdEvent upserted(HouseholdSnapshot snapshot) {
        return new HouseholdEvent(UUID.randomUUID(), HouseholdEvent.Type.HOUSEHOLD_UPSERTED, Instant.now(),
                snapshot.id(), snapshot);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static void eventually(Supplier<Boolean> condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (!condition.get()) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Condition not met in time");
            }
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("A demo family from Kafka is copied locally and gets a sample week once, even if the event repeats")
    void demoFamilyFromKafka() throws Exception {
        HouseholdEvent event = upserted(FamilyFixture.snapshot(1, true));
        publish(event);
        eventually(() -> count("select count(*) from demo_seed") == 1);

        int seeded = count("select count(*) from task");
        assertThat(seeded).isGreaterThan(5);
        assertThat(count("select count(*) from member_replica")).isEqualTo(4);

        publish(event); // redelivery
        publish(upserted(FamilyFixture.snapshot(2, true))); // a later change doesn't seed again
        eventually(() -> directory.find(HOUSEHOLD).map(h -> h.getVersion() == 2).orElse(false));
        assertThat(count("select count(*) from task")).isEqualTo(seeded);
    }

    @Test
    @DisplayName("An older snapshot never overwrites a newer one")
    void staleSnapshotsAreIgnored() throws Exception {
        publish(upserted(FamilyFixture.snapshot(5, false)));
        eventually(() -> directory.find(HOUSEHOLD).isPresent());

        var older = new HouseholdSnapshot(HOUSEHOLD, "Old name", true, false, 3, FamilyFixture.snapshot(3, false).members());
        publish(upserted(older));
        publish(upserted(new HouseholdSnapshot(HOUSEHOLD, "Newest", true, false, 6, older.members())));

        eventually(() -> directory.find(HOUSEHOLD).map(h -> h.getVersion() == 6).orElse(false));
        assertThat(directory.find(HOUSEHOLD).orElseThrow().getName()).isEqualTo("Newest");
    }

    @Test
    @DisplayName("Deleting a family in the Household service removes its tasks here")
    void deletedFamilyLosesItsTasks() throws Exception {
        publish(upserted(FamilyFixture.snapshot(1, true)));
        eventually(() -> count("select count(*) from task") > 0);

        publish(new HouseholdEvent(UUID.randomUUID(), HouseholdEvent.Type.HOUSEHOLD_DELETED, Instant.now(), HOUSEHOLD, null));

        eventually(() -> count("select count(*) from task") == 0);
        assertThat(directory.find(HOUSEHOLD)).isEmpty();
    }

    @Test
    @DisplayName("If the event hasn't arrived yet, the family is fetched from the Household service")
    void fallsBackToTheHouseholdService() throws Exception {
        householdServiceBody.set(json.writeValueAsString(FamilyFixture.snapshot(1, false)));

        mvc.perform(get("/api/week").with(parent()).header("X-Household-Id", HOUSEHOLD.toString())
                        .param("start", "2026-10-04"))
                .andExpect(status().isOk());

        assertThat(directory.find(HOUSEHOLD)).isPresent();
    }

    @Test
    void unknownFamilyIsForbidden() throws Exception {
        mvc.perform(get("/api/week").with(parent()).header("X-Household-Id", UUID.randomUUID().toString())
                        .param("start", "2026-10-04"))
                .andExpect(status().isForbidden());
    }

    private static HttpServer fakeHouseholdService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/internal/households/", exchange -> {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                String body = householdServiceBody.get();
                if (auth == null || !auth.startsWith("Bearer ")) {
                    exchange.sendResponseHeaders(401, -1);
                } else if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
