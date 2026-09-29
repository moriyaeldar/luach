package io.github.moriyaeldar.luach.tasks.api;

import io.github.moriyaeldar.luach.calendar.HebrewMonth;
import io.github.moriyaeldar.luach.calendar.LeapYearPolicy;
import io.github.moriyaeldar.luach.calendar.MissingDayPolicy;
import io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** A recurrence rule as sent and received by the API. Fields that don't apply to {@code kind} are ignored. */
public record RecurrenceDto(
        @NotNull RecurrenceKind kind,
        HebrewMonth hebrewMonth,
        @Min(1) @Max(30) Integer hebrewDay,
        LeapYearPolicy leapYearPolicy,
        MissingDayPolicy missingDayPolicy,
        Set<DayOfWeek> weekDays,
        @Min(1) @Max(31) Integer monthDay,
        LocalDate until) {

    public static RecurrenceDto none() {
        return new RecurrenceDto(RecurrenceKind.NONE, null, null, null, null, null, null, null);
    }
}
