package io.github.moriyaeldar.luach.calendar;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** How a task repeats. Hebrew rules follow the Hebrew calendar; Gregorian rules follow the civil one. */
public sealed interface RecurrenceRule {

    /** Every year on a Hebrew date, e.g. every 3 Tevet. */
    record HebrewYearly(HebrewMonth month, int day, LeapYearPolicy leapYearPolicy,
                        MissingDayPolicy missingDayPolicy) implements RecurrenceRule {
        public HebrewYearly {
            Objects.requireNonNull(month, "month");
            requireHebrewDay(day);
            leapYearPolicy = leapYearPolicy == null ? LeapYearPolicy.ADAR_II : leapYearPolicy;
            missingDayPolicy = missingDayPolicy == null ? MissingDayPolicy.NEXT_DAY : missingDayPolicy;
        }

        public HebrewYearly(HebrewMonth month, int day) {
            this(month, day, null, null);
        }
    }

    /** Every Hebrew month on the same day, e.g. every 1st of the month. */
    record HebrewMonthly(int day, MissingDayPolicy missingDayPolicy) implements RecurrenceRule {
        public HebrewMonthly {
            requireHebrewDay(day);
            missingDayPolicy = missingDayPolicy == null ? MissingDayPolicy.SKIP : missingDayPolicy;
        }
    }

    /** Every day of Rosh Chodesh (one or two days, depending on the length of the previous month). */
    record RoshChodesh() implements RecurrenceRule {
    }

    /** Every week on the given days. */
    record Weekly(Set<DayOfWeek> days) implements RecurrenceRule {
        public Weekly {
            if (days == null || days.isEmpty()) {
                throw new IllegalArgumentException("A weekly rule needs at least one day");
            }
            days = Set.copyOf(EnumSet.copyOf(days));
        }
    }

    /** Every Gregorian month on the same day. Months that are too short are skipped, as in RFC 5545. */
    record GregorianMonthly(int day) implements RecurrenceRule {
        public GregorianMonthly {
            if (day < 1 || day > 31) {
                throw new IllegalArgumentException("Day of month must be between 1 and 31: " + day);
            }
        }
    }

    private static void requireHebrewDay(int day) {
        if (day < 1 || day > 30) {
            throw new IllegalArgumentException("Hebrew day must be between 1 and 30: " + day);
        }
    }
}
