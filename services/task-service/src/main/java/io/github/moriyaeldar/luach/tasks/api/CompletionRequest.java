package io.github.moriyaeldar.luach.tasks.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Marks one occurrence done or not done. {@code date} is required for recurring tasks. */
public record CompletionRequest(LocalDate date, @NotNull Boolean done) {
}
