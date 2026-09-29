package io.github.moriyaeldar.luach.tasks;

import io.github.moriyaeldar.luach.tasks.domain.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TaskApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TaskRepository repository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String create(String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    @Test
    @DisplayName("The week shows Hebrew dates, day tasks and timed tasks on the right days")
    void weekView() throws Exception {
        create("""
                {"title":"Call the plumber","assigneeId":"aba","scheduleMode":"DAY","date":"2026-10-05","importance":2}""");
        create("""
                {"title":"Bring the permission slip","assigneeId":"noa","scheduleMode":"DAY","date":"2026-10-05","importance":5}""");
        create("""
                {"title":"Dentist","assigneeId":"noa","scheduleMode":"FIXED","date":"2026-10-06","startTime":"16:00","durationMinutes":45}""");
        create("""
                {"title":"Groceries","assigneeId":"aba","scheduleMode":"AUTO","durationMinutes":60,"deadline":"2026-10-08"}""");

        mvc.perform(get("/api/week").param("start", "2026-10-04"))
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
                {"title":"Grandma's birthday","assigneeId":"ima","scheduleMode":"DAY","date":"2026-01-01",
                 "recurrence":{"kind":"HEBREW_YEARLY","hebrewMonth":"TEVET","hebrewDay":3}}""");

        // 3 Tevet 5787 = 2026-12-13
        mvc.perform(get("/api/week").param("start", "2026-12-13"))
                .andExpect(jsonPath("$.days[0].dayTasks[0].title").value("Grandma's birthday"))
                .andExpect(jsonPath("$.days[0].dayTasks[0].recurring").value(true))
                .andExpect(jsonPath("$.days[0].dayTasks[0].done").value(false));

        mvc.perform(put("/api/tasks/{id}/completion", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-12-13\",\"done\":true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/week").param("start", "2026-12-13"))
                .andExpect(jsonPath("$.days[0].dayTasks[0].done").value(true));

        // Next year's occurrence (3 Tevet 5788 = 2028-01-02) is still open.
        mvc.perform(get("/api/week").param("start", "2027-12-27"))
                .andExpect(jsonPath("$.days[6].dayTasks[0].done").value(false));
    }

    @Test
    void completingARecurringTaskNeedsADate() throws Exception {
        String id = create("""
                {"title":"Rosh Chodesh prayer","assigneeId":"aba","scheduleMode":"DAY","date":"2026-01-01",
                 "recurrence":{"kind":"ROSH_CHODESH"}}""");
        mvc.perform(put("/api/tasks/{id}/completion", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewShowsNextOccurrencesWithHebrewDates() throws Exception {
        mvc.perform(post("/api/recurrence/preview").contentType(MediaType.APPLICATION_JSON).content("""
                        {"recurrence":{"kind":"HEBREW_YEARLY","hebrewMonth":"ADAR","hebrewDay":14},
                         "from":"2026-01-01","count":2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrences[*].date", contains("2026-03-03", "2027-03-23")))
                .andExpect(jsonPath("$.occurrences[1].hebrewDate.en").value("14 Adar II 5787"));
    }

    @Test
    void rejectsInvalidTasks() throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"No time","assigneeId":"aba","scheduleMode":"FIXED","date":"2026-10-05"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("startTime")));

        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"","assigneeId":"aba","scheduleMode":"DAY","date":"2026-10-05"}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Yearly without month","assigneeId":"aba","scheduleMode":"DAY","date":"2026-10-05",
                         "recurrence":{"kind":"HEBREW_YEARLY","hebrewDay":3}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("hebrewMonth")));
    }

    @Test
    void updateAndDelete() throws Exception {
        String id = create("""
                {"title":"Draft","assigneeId":"aba","scheduleMode":"DAY","date":"2026-10-05"}""");

        mvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Final","assigneeId":"ima","scheduleMode":"DAY","date":"2026-10-06"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.assigneeId").value("ima"));

        mvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void listsDemoMembers() throws Exception {
        mvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ima"));
    }
}
