package io.github.moriyaeldar.luach.calendar;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the weekly view shows in a day header.
 *
 * @param date          the Gregorian date
 * @param hebrewYear    e.g. 5787
 * @param hebrewMonth   the month as a user would name it that year
 * @param hebrewDay     1-30
 * @param hebrewDate    formatted, e.g. "ג׳ טבת תשפ״ז" / "3 Tevet 5787"
 * @param holidays      holidays and Rosh Chodesh on this day, possibly empty
 * @param parasha       the weekly Torah portion on Shabbat, otherwise {@code null}
 * @param shabbat       whether the day is Shabbat
 * @param restDay       Shabbat or a Yom Tov on which work is not done
 */
public record DayInfo(
        LocalDate date,
        int hebrewYear,
        HebrewMonth hebrewMonth,
        int hebrewDay,
        LocalizedText hebrewDate,
        List<LocalizedText> holidays,
        LocalizedText parasha,
        boolean shabbat,
        boolean restDay) {

    public DayInfo {
        holidays = List.copyOf(holidays);
    }
}
