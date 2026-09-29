package io.github.moriyaeldar.luach.calendar;

/**
 * What to do when the requested day does not exist in a given month, for example 30 Cheshvan in a year
 * where Cheshvan has only 29 days.
 */
public enum MissingDayPolicy {
    /** Move to the first day of the following month. */
    NEXT_DAY,
    /** Stay in the month and use its last day (the 29th). */
    LAST_DAY,
    /** Skip that month or year. */
    SKIP
}
