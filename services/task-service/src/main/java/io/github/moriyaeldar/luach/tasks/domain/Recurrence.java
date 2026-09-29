package io.github.moriyaeldar.luach.tasks.domain;

import io.github.moriyaeldar.luach.calendar.HebrewMonth;
import io.github.moriyaeldar.luach.calendar.LeapYearPolicy;
import io.github.moriyaeldar.luach.calendar.MissingDayPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Persistent form of a recurrence rule. Only the fields relevant to {@link #kind} are set. */
@Embeddable
public class Recurrence {

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_kind", nullable = false)
    private RecurrenceKind kind = RecurrenceKind.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "hebrew_month")
    private HebrewMonth hebrewMonth;

    @Column(name = "hebrew_day")
    private Integer hebrewDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "leap_year_policy")
    private LeapYearPolicy leapYearPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "missing_day_policy")
    private MissingDayPolicy missingDayPolicy;

    /** Comma separated {@link DayOfWeek} names, e.g. "SUNDAY,WEDNESDAY". */
    @Column(name = "week_days")
    private String weekDays;

    @Column(name = "month_day")
    private Integer monthDay;

    @Column(name = "recurrence_until")
    private LocalDate until;

    protected Recurrence() {
    }

    public Recurrence(RecurrenceKind kind, HebrewMonth hebrewMonth, Integer hebrewDay, LeapYearPolicy leapYearPolicy,
                      MissingDayPolicy missingDayPolicy, Set<DayOfWeek> weekDays, Integer monthDay, LocalDate until) {
        this.kind = kind == null ? RecurrenceKind.NONE : kind;
        this.hebrewMonth = hebrewMonth;
        this.hebrewDay = hebrewDay;
        this.leapYearPolicy = leapYearPolicy;
        this.missingDayPolicy = missingDayPolicy;
        this.weekDays = weekDays == null || weekDays.isEmpty() ? null
                : weekDays.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
        this.monthDay = monthDay;
        this.until = until;
    }

    public static Recurrence none() {
        return new Recurrence();
    }

    public boolean isRecurring() {
        return kind != RecurrenceKind.NONE;
    }

    public RecurrenceKind getKind() {
        return kind;
    }

    public HebrewMonth getHebrewMonth() {
        return hebrewMonth;
    }

    public Integer getHebrewDay() {
        return hebrewDay;
    }

    public LeapYearPolicy getLeapYearPolicy() {
        return leapYearPolicy;
    }

    public MissingDayPolicy getMissingDayPolicy() {
        return missingDayPolicy;
    }

    public Set<DayOfWeek> getWeekDays() {
        if (weekDays == null || weekDays.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(weekDays.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public Integer getMonthDay() {
        return monthDay;
    }

    public LocalDate getUntil() {
        return until;
    }
}
