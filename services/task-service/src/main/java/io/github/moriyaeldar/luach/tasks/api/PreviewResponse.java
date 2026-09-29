package io.github.moriyaeldar.luach.tasks.api;

import io.github.moriyaeldar.luach.calendar.LocalizedText;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record PreviewResponse(List<Item> occurrences) {

    public record Item(LocalDate date, DayOfWeek dayOfWeek, LocalizedText hebrewDate) {
    }
}
