package io.github.moriyaeldar.luach.tasks.api;

import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** A task as it appears on a specific day of the schedule. */
public record Occurrence(
        UUID taskId,
        String title,
        String assigneeId,
        ScheduleMode scheduleMode,
        LocalDate date,
        LocalTime startTime,
        Integer durationMinutes,
        int importance,
        boolean recurring,
        boolean done) {
}
