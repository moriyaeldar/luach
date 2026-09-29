package io.github.moriyaeldar.luach.tasks.service;

import io.github.moriyaeldar.luach.tasks.api.RecurrenceDto;
import io.github.moriyaeldar.luach.tasks.api.TaskRequest;
import io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind;
import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskValidationTest {

    private static final LocalDate DAY = LocalDate.parse("2026-10-05");

    private static TaskRequest request(ScheduleMode mode, LocalDate date, LocalTime time, Integer minutes,
                                       LocalDate deadline, RecurrenceDto recurrence) {
        return new TaskRequest("Task", null, "ima", mode, date, time, minutes, deadline, 3, recurrence);
    }

    @Test
    void fixedNeedsDateTimeAndDuration() {
        assertThatCode(() -> TaskService.validate(request(ScheduleMode.FIXED, DAY, LocalTime.NOON, 30, null, null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskService.validate(request(ScheduleMode.FIXED, DAY, null, 30, null, null)))
                .hasMessageContaining("startTime");
        assertThatThrownBy(() -> TaskService.validate(request(ScheduleMode.FIXED, DAY, LocalTime.NOON, null, null, null)))
                .hasMessageContaining("durationMinutes");
    }

    @Test
    void dayTaskNeedsOnlyADate() {
        assertThatCode(() -> TaskService.validate(request(ScheduleMode.DAY, DAY, null, null, null, null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskService.validate(request(ScheduleMode.DAY, null, null, null, null, null)))
                .hasMessageContaining("date");
    }

    @Test
    void autoNeedsDurationAndDeadlineAndCannotRepeatYet() {
        assertThatCode(() -> TaskService.validate(request(ScheduleMode.AUTO, null, null, 60, DAY, null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskService.validate(request(ScheduleMode.AUTO, null, null, 60, null, null)))
                .hasMessageContaining("deadline");
        var weekly = new RecurrenceDto(RecurrenceKind.ROSH_CHODESH, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> TaskService.validate(request(ScheduleMode.AUTO, null, null, 60, DAY, weekly)))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void untilMustNotBeBeforeTheStart() {
        var rule = new RecurrenceDto(RecurrenceKind.ROSH_CHODESH, null, null, null, null, null, null, DAY.minusDays(1));
        assertThatThrownBy(() -> TaskService.validate(request(ScheduleMode.DAY, DAY, null, null, null, rule)))
                .hasMessageContaining("until");
    }
}
