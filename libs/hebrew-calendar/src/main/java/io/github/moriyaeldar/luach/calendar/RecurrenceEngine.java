package io.github.moriyaeldar.luach.calendar;

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Expands a {@link RecurrenceRule} into the Gregorian dates it falls on within a range.
 *
 * <p>Stateless and thread-safe. The result is always sorted, distinct and inside {@code [from, to]}.
 */
public final class RecurrenceEngine {

    /** Guards against accidental huge expansions (about 100 years). */
    static final long MAX_RANGE_DAYS = 36_600;

    public List<LocalDate> occurrences(RecurrenceRule rule, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Range is too long, maximum is " + MAX_RANGE_DAYS + " days");
        }
        List<LocalDate> result = switch (rule) {
            case RecurrenceRule.HebrewYearly y -> hebrewYearly(y, from, to);
            case RecurrenceRule.HebrewMonthly m -> hebrewMonthly(m, from, to);
            case RecurrenceRule.RoshChodesh rc -> roshChodesh(from, to);
            case RecurrenceRule.Weekly w -> weekly(w, from, to);
            case RecurrenceRule.GregorianMonthly g -> gregorianMonthly(g, from, to);
        };
        return List.copyOf(result);
    }

    /** The date a yearly Hebrew rule falls on in a given Hebrew year, if any. */
    public Optional<LocalDate> resolveInYear(RecurrenceRule.HebrewYearly rule, int hebrewYear) {
        boolean leap = new JewishDate(hebrewYear, HebrewMonth.TISHREI.kosherJavaNumber(), 1).isJewishLeapYear();
        int month = switch (rule.month()) {
            case ADAR -> leap && rule.leapYearPolicy() == LeapYearPolicy.ADAR_II ? 13 : 12;
            case ADAR_I -> 12;
            case ADAR_II -> leap ? 13 : 12;
            default -> rule.month().kosherJavaNumber();
        };
        return resolveDay(hebrewYear, month, rule.day(), rule.missingDayPolicy());
    }

    private List<LocalDate> hebrewYearly(RecurrenceRule.HebrewYearly rule, LocalDate from, LocalDate to) {
        int firstYear = new JewishDate(from).getJewishYear();
        int lastYear = new JewishDate(to).getJewishYear();
        List<LocalDate> dates = new ArrayList<>();
        for (int year = firstYear; year <= lastYear; year++) {
            resolveInYear(rule, year).filter(d -> inRange(d, from, to)).ifPresent(dates::add);
        }
        return dates;
    }

    private List<LocalDate> hebrewMonthly(RecurrenceRule.HebrewMonthly rule, LocalDate from, LocalDate to) {
        JewishDate start = new JewishDate(from);
        JewishDate month = new JewishDate(start.getJewishYear(), start.getJewishMonth(), 1);
        List<LocalDate> dates = new ArrayList<>();
        while (!month.getLocalDate().isAfter(to)) {
            resolveDay(month.getJewishYear(), month.getJewishMonth(), rule.day(), rule.missingDayPolicy())
                    .filter(d -> inRange(d, from, to))
                    .ifPresent(dates::add);
            month = new JewishDate(month.getLocalDate().plusDays(month.getDaysInJewishMonth()));
        }
        return dates;
    }

    private List<LocalDate> roshChodesh(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (new JewishCalendar(d).isRoshChodesh()) {
                dates.add(d);
            }
        }
        return dates;
    }

    private List<LocalDate> weekly(RecurrenceRule.Weekly rule, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (rule.days().contains(d.getDayOfWeek())) {
                dates.add(d);
            }
        }
        return dates;
    }

    private List<LocalDate> gregorianMonthly(RecurrenceRule.GregorianMonthly rule, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (YearMonth ym = YearMonth.from(from); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) {
            if (rule.day() <= ym.lengthOfMonth()) {
                LocalDate d = ym.atDay(rule.day());
                if (inRange(d, from, to)) {
                    dates.add(d);
                }
            }
        }
        return dates;
    }

    private static Optional<LocalDate> resolveDay(int year, int month, int day, MissingDayPolicy policy) {
        int length = new JewishDate(year, month, 1).getDaysInJewishMonth();
        if (day <= length) {
            return Optional.of(new JewishDate(year, month, day).getLocalDate());
        }
        LocalDate lastDay = new JewishDate(year, month, length).getLocalDate();
        return switch (policy) {
            case NEXT_DAY -> Optional.of(lastDay.plusDays(1));
            case LAST_DAY -> Optional.of(lastDay);
            case SKIP -> Optional.empty();
        };
    }

    private static boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
        return !d.isBefore(from) && !d.isAfter(to);
    }
}
