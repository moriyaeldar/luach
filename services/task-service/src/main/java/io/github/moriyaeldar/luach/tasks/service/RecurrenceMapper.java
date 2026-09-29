package io.github.moriyaeldar.luach.tasks.service;

import io.github.moriyaeldar.luach.calendar.RecurrenceRule;
import io.github.moriyaeldar.luach.tasks.api.RecurrenceDto;
import io.github.moriyaeldar.luach.tasks.domain.Recurrence;
import io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind;

import java.util.Optional;

/** Converts between the API, the database and the calendar library's representation of a rule. */
final class RecurrenceMapper {

    private RecurrenceMapper() {
    }

    static Recurrence toEntity(RecurrenceDto dto) {
        if (dto == null || dto.kind() == RecurrenceKind.NONE) {
            return Recurrence.none();
        }
        toRule(dto); // validates the combination of fields
        return switch (dto.kind()) {
            case HEBREW_YEARLY -> new Recurrence(dto.kind(), dto.hebrewMonth(), dto.hebrewDay(),
                    dto.leapYearPolicy(), dto.missingDayPolicy(), null, null, dto.until());
            case HEBREW_MONTHLY -> new Recurrence(dto.kind(), null, dto.hebrewDay(), null, dto.missingDayPolicy(),
                    null, null, dto.until());
            case ROSH_CHODESH -> new Recurrence(dto.kind(), null, null, null, null, null, null, dto.until());
            case WEEKLY -> new Recurrence(dto.kind(), null, null, null, null, dto.weekDays(), null, dto.until());
            case GREGORIAN_MONTHLY -> new Recurrence(dto.kind(), null, null, null, null, null, dto.monthDay(),
                    dto.until());
            case NONE -> Recurrence.none();
        };
    }

    static RecurrenceDto toDto(Recurrence r) {
        if (r == null || !r.isRecurring()) {
            return RecurrenceDto.none();
        }
        return new RecurrenceDto(r.getKind(), r.getHebrewMonth(), r.getHebrewDay(), r.getLeapYearPolicy(),
                r.getMissingDayPolicy(), r.getWeekDays().isEmpty() ? null : r.getWeekDays(), r.getMonthDay(),
                r.getUntil());
    }

    static Optional<RecurrenceRule> toRule(Recurrence r) {
        return r == null || !r.isRecurring() ? Optional.empty() : Optional.of(toRule(toDto(r)));
    }

    static RecurrenceRule toRule(RecurrenceDto dto) {
        try {
            return switch (dto.kind()) {
                case HEBREW_YEARLY -> new RecurrenceRule.HebrewYearly(
                        require(dto.hebrewMonth(), "hebrewMonth"), require(dto.hebrewDay(), "hebrewDay"),
                        dto.leapYearPolicy(), dto.missingDayPolicy());
                case HEBREW_MONTHLY -> new RecurrenceRule.HebrewMonthly(
                        require(dto.hebrewDay(), "hebrewDay"), dto.missingDayPolicy());
                case ROSH_CHODESH -> new RecurrenceRule.RoshChodesh();
                case WEEKLY -> new RecurrenceRule.Weekly(dto.weekDays());
                case GREGORIAN_MONTHLY -> new RecurrenceRule.GregorianMonthly(require(dto.monthDay(), "monthDay"));
                case NONE -> throw new InvalidTaskException("A rule of kind NONE cannot be expanded");
            };
        } catch (IllegalArgumentException e) {
            throw new InvalidTaskException(e.getMessage());
        }
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new InvalidTaskException("'" + field + "' is required for this recurrence");
        }
        return value;
    }
}
