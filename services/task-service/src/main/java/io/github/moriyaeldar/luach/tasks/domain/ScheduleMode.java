package io.github.moriyaeldar.luach.tasks.domain;

/** How a task is placed in the schedule. */
public enum ScheduleMode {
    /** At a fixed date and time, e.g. "Dentist, Tuesday 16:00". */
    FIXED,
    /** On a date without a time, shown in the day's task area, e.g. "Call the plumber". */
    DAY,
    /** Placed automatically in a free slot before a deadline (scheduler arrives in milestone 3). */
    AUTO
}
