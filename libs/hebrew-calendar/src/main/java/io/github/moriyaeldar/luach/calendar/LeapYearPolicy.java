package io.github.moriyaeldar.luach.calendar;

/**
 * Where a date in (regular-year) Adar falls in a leap year, which has two Adars.
 * Customs differ between communities, so the user chooses.
 */
public enum LeapYearPolicy {
    /** The common custom for birthdays and Purim: observe in Adar II. */
    ADAR_II,
    /** Observe in Adar I. */
    ADAR_I
}
