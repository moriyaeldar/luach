package io.github.moriyaeldar.luach.calendar;

import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import io.github.moriyaeldar.luach.calendar.RecurrenceRule.GregorianMonthly;
import io.github.moriyaeldar.luach.calendar.RecurrenceRule.HebrewMonthly;
import io.github.moriyaeldar.luach.calendar.RecurrenceRule.HebrewYearly;
import io.github.moriyaeldar.luach.calendar.RecurrenceRule.RoshChodesh;
import io.github.moriyaeldar.luach.calendar.RecurrenceRule.Weekly;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceEngineTest {

    private final RecurrenceEngine engine = new RecurrenceEngine();

    private List<LocalDate> between(RecurrenceRule rule, String from, String to) {
        return engine.occurrences(rule, LocalDate.parse(from), LocalDate.parse(to));
    }

    @Nested
    @DisplayName("Yearly by Hebrew date")
    class Yearly {

        @Test
        void roshHashanahFallsOnKnownDates() {
            assertThat(between(new HebrewYearly(HebrewMonth.TISHREI, 1), "2025-01-01", "2026-12-31"))
                    .containsExactly(LocalDate.parse("2025-09-23"), LocalDate.parse("2026-09-12"));
        }

        @Test
        @DisplayName("Adar dates move to Adar II in leap years by default (Purim)")
        void adarDefaultsToAdarII() {
            // 5784 and 5787 are leap years; 5785 and 5786 are not.
            assertThat(between(new HebrewYearly(HebrewMonth.ADAR, 14), "2024-01-01", "2027-12-31"))
                    .containsExactly(
                            LocalDate.parse("2024-03-24"),
                            LocalDate.parse("2025-03-14"),
                            LocalDate.parse("2026-03-03"),
                            LocalDate.parse("2027-03-23"));
        }

        @Test
        @DisplayName("Adar dates can follow the Adar I custom instead")
        void adarWithAdarIPolicy() {
            var rule = new HebrewYearly(HebrewMonth.ADAR, 14, LeapYearPolicy.ADAR_I, MissingDayPolicy.NEXT_DAY);
            assertThat(between(rule, "2024-01-01", "2027-12-31"))
                    .containsExactly(
                            LocalDate.parse("2024-02-23"),
                            LocalDate.parse("2025-03-14"),
                            LocalDate.parse("2026-03-03"),
                            LocalDate.parse("2027-02-21"));
        }

        @Test
        @DisplayName("A date born in Adar II falls back to Adar in a regular year")
        void adarIIFallsBackToAdar() {
            assertThat(between(new HebrewYearly(HebrewMonth.ADAR_II, 14), "2025-01-01", "2025-12-31"))
                    .containsExactly(LocalDate.parse("2025-03-14"));
        }

        @Test
        @DisplayName("30 Adar I does not exist in a regular year; policy decides")
        void thirtyAdarIInRegularYear() {
            // 5786 (2025-26) is a regular year, Adar has 29 days.
            var next = new HebrewYearly(HebrewMonth.ADAR_I, 30, null, MissingDayPolicy.NEXT_DAY);
            var last = new HebrewYearly(HebrewMonth.ADAR_I, 30, null, MissingDayPolicy.LAST_DAY);
            var skip = new HebrewYearly(HebrewMonth.ADAR_I, 30, null, MissingDayPolicy.SKIP);
            assertThat(engine.resolveInYear(next, 5786)).contains(new JewishDate(5786, 1, 1).getLocalDate());
            assertThat(engine.resolveInYear(last, 5786)).contains(new JewishDate(5786, 12, 29).getLocalDate());
            assertThat(engine.resolveInYear(skip, 5786)).isEmpty();
        }

        @ParameterizedTest(name = "Hebrew year {0}")
        @CsvSource({"5780", "5781", "5782", "5783", "5784", "5785", "5786", "5787", "5788", "5789", "5790",
                "5791", "5792", "5793", "5794", "5795", "5796", "5797", "5798", "5799", "5800"})
        @DisplayName("30 Cheshvan follows the policy exactly when Cheshvan is short")
        void thirtyCheshvan(int year) {
            boolean cheshvanLong = new JewishDate(year, 8, 1).isCheshvanLong();
            LocalDate thirtieth = cheshvanLong ? new JewishDate(year, 8, 30).getLocalDate() : null;
            LocalDate firstOfKislev = new JewishDate(year, 9, 1).getLocalDate();
            LocalDate twentyNinth = new JewishDate(year, 8, 29).getLocalDate();

            var next = new HebrewYearly(HebrewMonth.CHESHVAN, 30, null, MissingDayPolicy.NEXT_DAY);
            var last = new HebrewYearly(HebrewMonth.CHESHVAN, 30, null, MissingDayPolicy.LAST_DAY);
            var skip = new HebrewYearly(HebrewMonth.CHESHVAN, 30, null, MissingDayPolicy.SKIP);

            if (cheshvanLong) {
                assertThat(engine.resolveInYear(next, year)).contains(thirtieth);
                assertThat(engine.resolveInYear(last, year)).contains(thirtieth);
                assertThat(engine.resolveInYear(skip, year)).contains(thirtieth);
            } else {
                assertThat(engine.resolveInYear(next, year)).contains(firstOfKislev);
                assertThat(engine.resolveInYear(last, year)).contains(twentyNinth);
                assertThat(engine.resolveInYear(skip, year)).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Monthly by Hebrew date")
    class Monthly {

        @Test
        void firstOfEveryMonthOverAYear() {
            // Hebrew year 5787 is a leap year: 13 months.
            LocalDate from = new JewishDate(5787, 7, 1).getLocalDate();
            LocalDate to = new JewishDate(5788, 7, 1).getLocalDate().minusDays(1);
            assertThat(engine.occurrences(new HebrewMonthly(1, null), from, to)).hasSize(13);
        }

        @Test
        @DisplayName("Day 30 with SKIP only appears in 30-day months")
        void thirtiethSkipsShortMonths() {
            LocalDate from = new JewishDate(5786, 7, 1).getLocalDate();
            LocalDate to = new JewishDate(5787, 7, 1).getLocalDate().minusDays(1);
            List<LocalDate> dates = engine.occurrences(new HebrewMonthly(30, MissingDayPolicy.SKIP), from, to);
            assertThat(dates).allSatisfy(d -> assertThat(new JewishDate(d).getJewishDayOfMonth()).isEqualTo(30));
            // A regular year has 12 months; 6 or 7 of them have 30 days depending on Cheshvan and Kislev.
            assertThat(dates).hasSizeBetween(5, 7);
        }
    }

    @Test
    @DisplayName("Rosh Chodesh Cheshvan 5787 is two days")
    void roshChodeshTwoDays() {
        assertThat(between(new RoshChodesh(), "2026-10-01", "2026-10-31"))
                .containsExactly(LocalDate.parse("2026-10-11"), LocalDate.parse("2026-10-12"));
    }

    @Test
    void weeklyOnSundayAndWednesday() {
        var rule = new Weekly(Set.of(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY));
        assertThat(between(rule, "2026-09-27", "2026-10-07"))
                .containsExactly(
                        LocalDate.parse("2026-09-27"), LocalDate.parse("2026-09-30"),
                        LocalDate.parse("2026-10-04"), LocalDate.parse("2026-10-07"));
    }

    @Test
    @DisplayName("Gregorian monthly on the 31st skips short months")
    void gregorianMonthlySkipsShortMonths() {
        assertThat(between(new GregorianMonthly(31), "2026-01-01", "2026-06-30"))
                .containsExactly(
                        LocalDate.parse("2026-01-31"), LocalDate.parse("2026-03-31"), LocalDate.parse("2026-05-31"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> new HebrewYearly(HebrewMonth.NISAN, 31)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> between(new RoshChodesh(), "2026-02-01", "2026-01-01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> between(new RoshChodesh(), "1900-01-01", "2100-01-01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Property: every yearly rule gives sorted dates in range that map back to the requested day")
    void yearlyProperties() {
        var random = new java.util.Random(5787);
        HebrewMonth[] months = HebrewMonth.values();
        MissingDayPolicy[] policies = MissingDayPolicy.values();
        IntStream.range(0, 400).forEach(i -> {
            HebrewMonth month = months[random.nextInt(months.length)];
            int day = 1 + random.nextInt(30);
            MissingDayPolicy policy = policies[random.nextInt(policies.length)];
            LocalDate from = LocalDate.of(1990, 1, 1).plusDays(random.nextInt(20_000));
            LocalDate to = from.plusDays(random.nextInt(3_000));
            var rule = new HebrewYearly(month, day, LeapYearPolicy.ADAR_II, policy);

            List<LocalDate> dates = engine.occurrences(rule, from, to);

            assertThat(dates).isSorted().doesNotHaveDuplicates()
                    .allSatisfy(d -> assertThat(d).isBetween(from, to));
            for (LocalDate d : dates) {
                JewishDate jd = new JewishDate(d);
                if (jd.getJewishDayOfMonth() == day) {
                    HebrewMonth actual = HebrewMonth.fromKosherJava(jd.getJewishMonth(), jd.isJewishLeapYear());
                    assertThat(actual.displayName().en()).as("month for %s", rule)
                            .startsWith(month.displayName().en().split(" ")[0]);
                } else {
                    // Only a missing day may land elsewhere, and only as the 29th or the 1st.
                    assertThat(day).isEqualTo(30);
                    assertThat(policy).isNotEqualTo(MissingDayPolicy.SKIP);
                    assertThat(jd.getJewishDayOfMonth()).isIn(1, 29);
                }
            }
        });
    }
}
