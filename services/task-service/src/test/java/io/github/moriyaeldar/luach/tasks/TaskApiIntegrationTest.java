package io.github.moriyaeldar.luach.tasks;

import com.jayway.jsonpath.JsonPath;
import io.github.moriyaeldar.luach.tasks.household.HouseholdDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static io.github.moriyaeldar.luach.tasks.FamilyFixture.CHILD;
import static io.github.moriyaeldar.luach.tasks.FamilyFixture.HOUSEHOLD;
import static io.github.moriyaeldar.luach.tasks.FamilyFixture.PARENT;
import static io.github.moriyaeldar.luach.tasks.FamilyFixture.PARTNER;
import static io.github.moriyaeldar.luach.tasks.FamilyFixture.as;
import static io.github.moriyaeldar.luach.tasks.FamilyFixture.parent;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "luach.kafka.enabled=false")
class TaskApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private HouseholdDirectory directory;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc.execute("truncate task, task_completion, household_replica, member_replica, processed_event, demo_seed cascade");
        directory.apply(FamilyFixture.snapshot(1, false));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static MockHttpServletRequestBuilder family(MockHttpServletRequestBuilder request) {
        return request.header("X-Household-Id", HOUSEHOLD.toString());
    }

    private String create(RequestPostProcessor who, String json) throws Exception {
        MvcResult result = mvc.perform(family(post("/api/tasks")).with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String create(String json) throws Exception {
        return create(parent(), json);
    }

    @Test
    @DisplayName("The week shows Hebrew dates, day tasks and timed tasks on the right days")
    void weekView() throws Exception {
        create("""
                {"title":"Call the plumber","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05","importance":2}"""
                .formatted(PARTNER));
        create("""
                {"title":"Bring the permission slip","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05","importance":5}"""
                .formatted(CHILD));
        create("""
                {"title":"Dentist","assigneeId":"%s","scheduleMode":"FIXED","date":"2026-10-06","startTime":"16:00","durationMinutes":45}"""
                .formatted(CHILD));
        create("""
                {"title":"Groceries","assigneeId":"%s","scheduleMode":"AUTO","durationMinutes":60,"deadline":"2026-10-08"}"""
                .formatted(PARTNER));

        mvc.perform(family(get("/api/week")).with(parent()).param("start", "2026-10-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].info.hebrewDate.en").value("23 Tishrei 5787"))
                .andExpect(jsonPath("$.days[1].dayTasks[*].title",
                        contains("Bring the permission slip", "Call the plumber")))
                .andExpect(jsonPath("$.days[1].timedTasks", empty()))
                .andExpect(jsonPath("$.days[2].timedTasks[0].title").value("Dentist"))
                .andExpect(jsonPath("$.days[6].info.parasha.he").value("בראשית"))
                .andExpect(jsonPath("$.unscheduled[0].title").value("Groceries"));
    }

    @Test
    @DisplayName("A yearly Hebrew-date task appears on its Hebrew date, and each occurrence is completed separately")
    void recurringHebrewBirthday() throws Exception {
        String id = create("""
                {"title":"Grandma's birthday","assigneeId":"%s","scheduleMode":"DAY","date":"2026-01-01",
                 "recurrence":{"kind":"HEBREW_YEARLY","hebrewMonth":"TEVET","hebrewDay":3}}""".formatted(PARENT));

        mvc.perform(family(get("/api/week")).with(parent()).param("start", "2026-12-13"))
                .andExpect(jsonPath("$.days[0].dayTasks[0].title").value("Grandma's birthday"))
                .andExpect(jsonPath("$.days[0].dayTasks[0].recurring").value(true))
                .andExpect(jsonPath("$.days[0].dayTasks[0].done").value(false));

        mvc.perform(family(put("/api/tasks/{id}/completion", id)).with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-12-13\",\"done\":true}"))
                .andExpect(status().isOk());

        mvc.perform(family(get("/api/week")).with(parent()).param("start", "2026-12-13"))
                .andExpect(jsonPath("$.days[0].dayTasks[0].done").value(true));
        mvc.perform(family(get("/api/week")).with(parent()).param("start", "2027-12-27"))
                .andExpect(jsonPath("$.days[6].dayTasks[0].done").value(false));
    }

    @Test
    void completingARecurringTaskNeedsADate() throws Exception {
        String id = create("""
                {"title":"Rosh Chodesh","assigneeId":"%s","scheduleMode":"DAY","date":"2026-01-01",
                 "recurrence":{"kind":"ROSH_CHODESH"}}""".formatted(PARENT));
        mvc.perform(family(put("/api/tasks/{id}/completion", id)).with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewShowsNextOccurrencesWithHebrewDates() throws Exception {
        mvc.perform(family(post("/api/recurrence/preview")).with(parent()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"recurrence":{"kind":"HEBREW_YEARLY","hebrewMonth":"ADAR","hebrewDay":14},
                         "from":"2026-01-01","count":2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrences[*].date", contains("2026-03-03", "2027-03-23")))
                .andExpect(jsonPath("$.occurrences[1].hebrewDate.en").value("14 Adar II 5787"));
    }

    @Test
    void rejectsInvalidTasks() throws Exception {
        mvc.perform(family(post("/api/tasks")).with(parent()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"No time","assigneeId":"%s","scheduleMode":"FIXED","date":"2026-10-05"}""".formatted(PARENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("startTime")));

        mvc.perform(family(post("/api/tasks")).with(parent()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05"}""".formatted(PARENT)))
                .andExpect(status().isBadRequest());

        mvc.perform(family(post("/api/tasks")).with(parent()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Stranger","assigneeId":"someone-else","scheduleMode":"DAY","date":"2026-10-05"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("member of this family")));

        mvc.perform(post("/api/tasks").with(parent()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete() throws Exception {
        String id = create("""
                {"title":"Draft","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05"}""".formatted(PARENT));

        mvc.perform(family(put("/api/tasks/{id}", id)).with(parent()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Final","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-06"}""".formatted(PARTNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.assigneeId").value(PARTNER.toString()));

        mvc.perform(family(delete("/api/tasks/{id}", id)).with(parent())).andExpect(status().isNoContent());
        mvc.perform(family(get("/api/tasks/{id}", id)).with(parent())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Only family members get in; children see the schedule and tick off only their own tasks")
    void permissions() throws Exception {
        String childTask = create("""
                {"title":"Homework","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05"}""".formatted(CHILD));
        String partnerTask = create("""
                {"title":"Taxes","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05"}""".formatted(PARTNER));
        var child = as("google:child");

        mvc.perform(family(get("/api/week")).param("start", "2026-10-04")).andExpect(status().isUnauthorized());
        mvc.perform(family(get("/api/week")).with(as("google:stranger")).param("start", "2026-10-04"))
                .andExpect(status().isForbidden());

        mvc.perform(family(get("/api/week")).with(child).param("start", "2026-10-04")).andExpect(status().isOk());
        mvc.perform(family(post("/api/tasks")).with(child).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Party","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05"}""".formatted(CHILD)))
                .andExpect(status().isForbidden());
        mvc.perform(family(put("/api/tasks/{id}/completion", childTask)).with(child)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
                .andExpect(status().isOk());
        mvc.perform(family(put("/api/tasks/{id}/completion", partnerTask)).with(child)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
                .andExpect(status().isForbidden());

        // The partner (a regular member) can edit the schedule.
        create(as("google:partner"), """
                {"title":"Groceries","assigneeId":"%s","scheduleMode":"DAY","date":"2026-10-05"}""".formatted(PARTNER));
    }
}
