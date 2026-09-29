package io.github.moriyaeldar.luach.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HebrewCalendarTest {

    private final HebrewCalendar israel = new HebrewCalendar(true);

    @Test
    void roshHashanah5787() {
        DayInfo day = israel.describe(LocalDate.parse("2026-09-12"));

        assertThat(day.hebrewYear()).isEqualTo(5787);
        assertThat(day.hebrewMonth()).isEqualTo(HebrewMonth.TISHREI);
        assertThat(day.hebrewDay()).isEqualTo(1);
        assertThat(day.hebrewDate().en()).isEqualTo("1 Tishrei 5787");
        assertThat(day.hebrewDate().he()).contains("תשרי");
        assertThat(day.holidays()).extracting(LocalizedText::en).contains("Rosh Hashanah");
        assertThat(day.restDay()).isTrue();
        assertThat(day.shabbat()).isTrue();
    }

    @Test
    void roshChodeshIsListedAsAHoliday() {
        DayInfo day = israel.describe(LocalDate.parse("2026-10-12"));
        assertThat(day.holidays()).extracting(LocalizedText::en).anyMatch(s -> s.contains("Rosh Chodesh"));
        assertThat(day.restDay()).isFalse();
    }

    @Test
    void parashaOnlyOnShabbat() {
        assertThat(israel.describe(LocalDate.parse("2026-10-10")).parasha()).isNotNull();
        assertThat(israel.describe(LocalDate.parse("2026-10-09")).parasha()).isNull();
    }

    @Test
    void adarIInALeapYear() {
        // 5787 is a leap year; 14 Adar I = 2027-02-21.
        DayInfo day = israel.describe(LocalDate.parse("2027-02-21"));
        assertThat(day.hebrewMonth()).isEqualTo(HebrewMonth.ADAR_I);
        assertThat(day.hebrewDate().en()).isEqualTo("14 Adar I 5787");
    }

    @Test
    void secondDayOfYomTovOnlyAbroad() {
        // 16 Nisan 5786 = Friday 2026-04-03: Yom Tov abroad, Chol HaMoed in Israel.
        LocalDate secondDayPesach = new com.kosherjava.zmanim.hebrewcalendar.JewishDate(5786, 1, 16).getLocalDate();
        assertThat(secondDayPesach).isEqualTo(LocalDate.parse("2026-04-03"));
        assertThat(israel.describe(secondDayPesach).restDay()).isFalse();
        assertThat(new HebrewCalendar(false).describe(secondDayPesach).restDay()).isTrue();
    }

    @Test
    void weekHasSevenConsecutiveDays() {
        List<DayInfo> week = israel.week(LocalDate.parse("2026-09-27"));
        assertThat(week).hasSize(7);
        assertThat(week.get(6).date()).isEqualTo(LocalDate.parse("2026-10-03"));
    }
}
