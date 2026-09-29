package io.github.moriyaeldar.luach.tasks.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PreviewRequest(@NotNull @Valid RecurrenceDto recurrence, @NotNull LocalDate from,
                             @Min(1) @Max(20) Integer count) {
}
