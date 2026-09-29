package io.github.moriyaeldar.luach.tasks.api;

import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;
import io.github.moriyaeldar.luach.tasks.domain.TaskStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String notes,
        String assigneeId,
        ScheduleMode scheduleMode,
        LocalDate date,
        LocalTime startTime,
        Integer durationMinutes,
        LocalDate deadline,
        int importance,
        TaskStatus status,
        RecurrenceDto recurrence,
        long version) {
}
