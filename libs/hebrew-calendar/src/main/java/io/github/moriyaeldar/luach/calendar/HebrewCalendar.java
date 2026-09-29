package io.github.moriyaeldar.luach.calendar;

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Describes Gregorian dates in Hebrew-calendar terms, in Hebrew and English. */
public final class HebrewCalendar {

    /** Modern Israeli spellings, in the order KosherJava expects. */
    private static final String[] ENGLISH_MONTHS = {
            "Nisan", "Iyar", "Sivan", "Tammuz", "Av", "Elul", "Tishrei", "Cheshvan", "Kislev", "Tevet",
            "Shevat", "Adar", "Adar II", "Adar I"};

    private static final String[] ENGLISH_HOLIDAYS = {
            "Erev Pesach", "Pesach", "Chol HaMoed Pesach", "Pesach Sheni", "Erev Shavuot", "Shavuot",
            "17th of Tammuz", "Tisha B'Av", "Tu B'Av", "Erev Rosh Hashanah", "Rosh Hashanah", "Fast of Gedaliah",
            "Erev Yom Kippur", "Yom Kippur", "Erev Sukkot", "Sukkot", "Chol HaMoed Sukkot", "Hoshana Rabbah",
            "Shemini Atzeret", "Simchat Torah", "Erev Chanukah", "Chanukah", "10th of Tevet", "Tu BiShvat",
            "Fast of Esther", "Purim", "Shushan Purim", "Purim Katan", "Rosh Chodesh", "Yom HaShoah",
            "Yom HaZikaron", "Yom HaAtzmaut", "Yom Yerushalayim", "Lag BaOmer", "Shushan Purim Katan",
            "Isru Chag"};

    private final boolean inIsrael;

    /** @param inIsrael Israel observes one day of Yom Tov and a different parasha schedule at times. */
    public HebrewCalendar(boolean inIsrael) {
        this.inIsrael = inIsrael;
    }

    public DayInfo describe(LocalDate date) {
        JewishCalendar jc = new JewishCalendar(date);
        jc.setInIsrael(inIsrael);
        jc.setUseModernHolidays(true);

        HebrewDateFormatter he = hebrewFormatter();
        HebrewDateFormatter en = englishFormatter();

        HebrewMonth month = HebrewMonth.fromKosherJava(jc.getJewishMonth(), jc.isJewishLeapYear());
        LocalizedText hebrewDate = new LocalizedText(
                he.format(jc),
                jc.getJewishDayOfMonth() + " " + month.displayName().en() + " " + jc.getJewishYear());

        List<LocalizedText> holidays = new ArrayList<>();
        addIfPresent(holidays, he.formatYomTov(jc), en.formatYomTov(jc));
        if (jc.isRoshChodesh()) {
            addIfPresent(holidays, he.formatRoshChodesh(jc), en.formatRoshChodesh(jc));
        }

        boolean shabbat = date.getDayOfWeek() == DayOfWeek.SATURDAY;
        LocalizedText parasha = null;
        if (shabbat && jc.getParshah() != JewishCalendar.Parsha.NONE) {
            parasha = new LocalizedText(he.formatParsha(jc), en.formatParsha(jc));
        }

        return new DayInfo(date, jc.getJewishYear(), month, jc.getJewishDayOfMonth(), hebrewDate,
                holidays, parasha, shabbat, jc.isAssurBemelacha());
    }

    /** Seven consecutive days starting at {@code start}. */
    public List<DayInfo> week(LocalDate start) {
        List<DayInfo> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            days.add(describe(start.plusDays(i)));
        }
        return days;
    }

    private static void addIfPresent(List<LocalizedText> list, String he, String en) {
        if (he != null && !he.isBlank()) {
            list.add(new LocalizedText(he, en));
        }
    }

    private static HebrewDateFormatter hebrewFormatter() {
        HebrewDateFormatter f = new HebrewDateFormatter();
        f.setHebrewFormat(true);
        f.setUseGershGershayim(true);
        return f;
    }

    private static HebrewDateFormatter englishFormatter() {
        HebrewDateFormatter f = new HebrewDateFormatter();
        f.setTransliteratedMonthList(ENGLISH_MONTHS);
        f.setTransliteratedHolidayList(ENGLISH_HOLIDAYS);
        return f;
    }
}
