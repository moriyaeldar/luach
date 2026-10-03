package io.github.moriyaeldar.luach.household;

import com.jayway.jsonpath.JsonPath;
import io.github.moriyaeldar.luach.events.HouseholdEvent;
import io.github.moriyaeldar.luach.household.outbox.OutboxRelay;
import io.github.moriyaeldar.luach.household.outbox.OutboxRepository;
import io.github.moriyaeldar.luach.household.service.DemoCleanup;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "luach.outbox.poll-ms=3600000" // relayed explicitly in the tests
})
@EmbeddedKafka(partitions = 2, topics = HouseholdEvent.TOPIC)
class HouseholdApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private OutboxRelay relay;
    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private DemoCleanup demoCleanup;
    @Autowired
    private EmbeddedKafkaBroker broker;
    @Autowired
    private JsonMapper json;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc.execute("truncate outbox_event, invite, member, household, app_user cascade");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static RequestPostProcessor user(String subject, String name) {
        return jwt().jwt(j -> j.subject(subject).claim("name", name).claim("email", name.toLowerCase() + "@example.com"));
    }

    private static RequestPostProcessor demoUser(String subject) {
        return jwt().jwt(j -> j.subject(subject).claim("name", "Demo").claim("demo", true));
    }

    private String createHousehold(RequestPostProcessor who, String name) throws Exception {
        String body = mvc.perform(post("/api/households").with(who).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"inIsrael\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("First login creates the user; a family is created with the creator as admin")
    void createFamily() throws Exception {
        var moriya = user("google:1", "Moriya");
        mvc.perform(get("/api/me").with(moriya))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Moriya"))
                .andExpect(jsonPath("$.households", hasSize(0)));

        String id = createHousehold(moriya, "משפחת אלדר");

        mvc.perform(get("/api/me").with(moriya))
                .andExpect(jsonPath("$.households[0].name").value("משפחת אלדר"))
                .andExpect(jsonPath("$.households[0].role").value("ADMIN"));
        mvc.perform(get("/api/households/{id}/members", id).with(moriya))
                .andExpect(jsonPath("$[0].displayName").value("Moriya"))
                .andExpect(jsonPath("$[0].connected").value(true));
    }

    @Test
    @DisplayName("A demo visitor gets a private sample family with four members, once")
    void demoFamily() throws Exception {
        var visitor = demoUser("demo:abc");
        mvc.perform(get("/api/me").with(visitor))
                .andExpect(jsonPath("$.user.demo").value(true))
                .andExpect(jsonPath("$.households", hasSize(1)))
                .andExpect(jsonPath("$.households[0].demo").value(true));
        mvc.perform(get("/api/me").with(visitor)).andExpect(jsonPath("$.households", hasSize(1)));

        String id = JsonPath.read(mvc.perform(get("/api/me").with(visitor)).andReturn().getResponse()
                .getContentAsString(), "$.households[0].id");
        mvc.perform(get("/api/households/{id}/members", id).with(visitor))
                .andExpect(jsonPath("$[*].role", contains("ADMIN", "MEMBER", "CHILD", "CHILD")));
    }

    @Test
    @DisplayName("Only members see a family, and only admins manage it")
    void permissions() throws Exception {
        var moriya = user("google:1", "Moriya");
        var stranger = user("google:2", "Stranger");
        String id = createHousehold(moriya, "Family");

        mvc.perform(get("/api/households/{id}/members", id).with(stranger)).andExpect(status().isForbidden());
        mvc.perform(get("/api/households/{id}/members", id)).andExpect(status().isUnauthorized());

        // Add a regular member, connect the stranger to it, and check they can't manage the family.
        String dad = JsonPath.read(mvc.perform(post("/api/households/{id}/members", id).with(moriya)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"אבא\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        String token = invite(moriya, id, "{\"memberId\":\"" + dad + "\"}");
        mvc.perform(post("/api/invites/{t}/accept", token).with(stranger)).andExpect(status().isOk());

        mvc.perform(post("/api/households/{id}/members", id).with(stranger)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An invite link connects a person to an existing member, once")
    void inviteExistingMember() throws Exception {
        var moriya = user("google:1", "Moriya");
        var dad = user("google:2", "Yossi");
        var someoneElse = user("google:3", "Other");
        String id = createHousehold(moriya, "Family");
        String dadMember = JsonPath.read(mvc.perform(post("/api/households/{id}/members", id).with(moriya)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"אבא\"}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        String token = invite(moriya, id, "{\"memberId\":\"" + dadMember + "\"}");

        mvc.perform(get("/api/invites/{t}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.householdName").value("Family"))
                .andExpect(jsonPath("$.displayName").value("אבא"))
                .andExpect(jsonPath("$.status").value("VALID"));

        mvc.perform(post("/api/invites/{t}/accept", token).with(dad))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(dadMember))
                .andExpect(jsonPath("$.role").value("MEMBER"));
        mvc.perform(post("/api/invites/{t}/accept", token).with(dad)).andExpect(status().isOk()); // idempotent
        mvc.perform(post("/api/invites/{t}/accept", token).with(someoneElse)).andExpect(status().isGone());
        mvc.perform(get("/api/invites/{t}", token)).andExpect(jsonPath("$.status").value("USED"));

        mvc.perform(get("/api/households/{id}/members", id).with(moriya))
                .andExpect(jsonPath("$[1].connected").value(true))
                .andExpect(jsonPath("$[1].email").value("yossi@example.com"));
    }

    @Test
    void inviteForANewMember() throws Exception {
        var moriya = user("google:1", "Moriya");
        String id = createHousehold(moriya, "Family");
        String token = invite(moriya, id, "{\"displayName\":\"סבתא\",\"role\":\"MEMBER\"}");

        mvc.perform(post("/api/invites/{t}/accept", token).with(user("google:9", "Grandma")))
                .andExpect(jsonPath("$.role").value("MEMBER"));
        mvc.perform(get("/api/households/{id}/members", id).with(moriya))
                .andExpect(jsonPath("$[*].displayName", contains("Moriya", "סבתא")));
    }

    @Test
    void aFamilyAlwaysKeepsAnAdmin() throws Exception {
        var moriya = user("google:1", "Moriya");
        String id = createHousehold(moriya, "Family");
        String me = JsonPath.read(mvc.perform(get("/api/me").with(moriya)).andReturn().getResponse()
                .getContentAsString(), "$.households[0].memberId");

        mvc.perform(patch("/api/households/{id}/members/{m}", id, me).with(moriya)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/households/{id}/members/{m}", id, me).with(moriya))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Changes reach Kafka through the outbox as versioned snapshots")
    void changesArePublishedToKafka() throws Exception {
        var moriya = user("google:1", "Moriya");
        String id = createHousehold(moriya, "Family");
        mvc.perform(post("/api/households/{id}/members", id).with(moriya)
                .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"נועה\",\"role\":\"CHILD\"}"));
        assertThat(outbox.countByPublishedAtIsNull()).isEqualTo(2);

        relay.relay();

        assertThat(outbox.countByPublishedAtIsNull()).isZero();
        List<HouseholdEvent> events = consumeAll(2);
        assertThat(events).extracting(HouseholdEvent::type).containsOnly(HouseholdEvent.Type.HOUSEHOLD_UPSERTED);
        assertThat(events).extracting(e -> e.snapshot().version()).containsExactly(1L, 2L);
        assertThat(events.get(1).snapshot().members()).extracting(m -> m.displayName())
                .containsExactly("Moriya", "נועה");
        assertThat(events.get(0).snapshot().members().getFirst().userSubject()).isEqualTo("google:1");
    }

    @Test
    void internalSnapshotNeedsAServiceToken() throws Exception {
        var moriya = user("google:1", "Moriya");
        String id = createHousehold(moriya, "Family");

        mvc.perform(get("/internal/households/{id}", id).with(moriya)).andExpect(status().isForbidden());
        mvc.perform(get("/internal/households/{id}", id)
                        .with(jwt().jwt(j -> j.subject("service:task-service"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_internal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].userSubject").value("google:1"));
    }

    @Test
    void expiredDemoFamiliesAreDeleted() throws Exception {
        mvc.perform(get("/api/me").with(demoUser("demo:old")));
        jdbc.update("update household set created_at = now() - interval '2 days'");

        assertThat(demoCleanup.deleteExpired()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from member", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from outbox_event where type = 'HOUSEHOLD_DELETED'",
                Integer.class)).isEqualTo(1);
    }

    private String invite(RequestPostProcessor admin, String householdId, String body) throws Exception {
        return JsonPath.read(mvc.perform(post("/api/households/{id}/invites", householdId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private List<HouseholdEvent> consumeAll(int expected) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "test-" + System.nanoTime(), false);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, HouseholdEvent.TOPIC);
            List<HouseholdEvent> events = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 10_000;
            while (events.size() < expected && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                    events.add(json.readValue(r.value(), HouseholdEvent.class));
                }
            }
            events.sort((a, b) -> Long.compare(a.snapshot().version(), b.snapshot().version()));
            return events;
        }
    }
}
