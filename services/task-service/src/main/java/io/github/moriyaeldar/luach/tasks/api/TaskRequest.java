package io.github.moriyaeldar.luach.tasks.api;

import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String notes,
        @NotBlank @Size(max = 64) String assigneeId,
        @NotNull ScheduleMode scheduleMode,
        LocalDate date,
        LocalTime startTime,
        @Min(5) @Max(24 * 60) Integer durationMinutes,
        LocalDate deadline,
        @Min(1) @Max(5) Integer importance,
        @Valid RecurrenceDto recurrence) {
}
