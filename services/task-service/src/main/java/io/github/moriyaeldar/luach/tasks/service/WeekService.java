package io.github.moriyaeldar.luach.tasks.service;

import io.github.moriyaeldar.luach.calendar.HebrewCalendar;
import io.github.moriyaeldar.luach.calendar.RecurrenceEngine;
import io.github.moriyaeldar.luach.calendar.RecurrenceRule;
import io.github.moriyaeldar.luach.tasks.api.Occurrence;
import io.github.moriyaeldar.luach.tasks.api.PreviewRequest;
import io.github.moriyaeldar.luach.tasks.api.PreviewResponse;
import io.github.moriyaeldar.luach.tasks.api.WeekResponse;
import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;
import io.github.moriyaeldar.luach.tasks.domain.Task;
import io.github.moriyaeldar.luach.tasks.domain.TaskCompletion;
import io.github.moriyaeldar.luach.tasks.domain.TaskCompletionRepository;
import io.github.moriyaeldar.luach.tasks.domain.TaskRepository;
import io.github.moriyaeldar.luach.tasks.domain.TaskStatus;
import io.github.moriyaeldar.luach.tasks.household.HouseholdAccess;
import io.github.moriyaeldar.luach.tasks.household.HouseholdAccess.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Builds the weekly schedule: Hebrew-calendar info per day plus every task occurrence that falls on it. */
@Service
@Transactional(readOnly = true)
public class WeekService {

    private static final Comparator<Occurrence> DAY_TASK_ORDER = Comparator
            .comparing(Occurrence::done)
            .thenComparing(Occurrence::importance, Comparator.reverseOrder())
            .thenComparing(Occurrence::title);

    private static final Comparator<Occurrence> TIMED_ORDER = Comparator
            .comparing(Occurrence::startTime, Comparator.nullsLast(LocalTime::compareTo))
            .thenComparing(Occurrence::title);

    private final TaskRepository tasks;
    private final TaskCompletionRepository completions;
    private final RecurrenceEngine engine = new RecurrenceEngine();
    private final HouseholdAccess access;

    public WeekService(TaskRepository tasks, TaskCompletionRepository completions, HouseholdAccess access) {
        this.tasks = tasks;
        this.completions = completions;
        this.access = access;
    }

    public WeekResponse week(UUID householdId, LocalDate start) {
        var household = access.require(householdId, Permission.READ).household();
        HebrewCalendar calendar = new HebrewCalendar(household.isInIsrael());
        LocalDate end = start.plusDays(6);
        Map<LocalDate, List<Occurrence>> byDay = new HashMap<>();

        for (Task task : tasks.findOneOffBetween(householdId, start, end)) {
            add(byDay, occurrence(task, task.getDate(), task.getStatus() == TaskStatus.DONE));
        }

        List<Task> recurring = tasks.findRecurringActiveBetween(householdId, start, end);
        Set<TaskCompletion.Key> done = recurring.isEmpty() ? Set.of()
                : completions.findForTasksBetween(recurring.stream().map(Task::getId).toList(), start, end)
                        .stream().map(TaskCompletion::getKey).collect(Collectors.toSet());
        for (Task task : recurring) {
            LocalDate from = task.getDate().isAfter(start) ? task.getDate() : start;
            LocalDate until = task.getRecurrence().getUntil();
            LocalDate to = until != null && until.isBefore(end) ? until : end;
            if (from.isAfter(to)) {
                continue;
            }
            RecurrenceRule rule = RecurrenceMapper.toRule(task.getRecurrence()).orElseThrow();
            for (LocalDate date : engine.occurrences(rule, from, to)) {
                add(byDay, occurrence(task, date, done.contains(new TaskCompletion.Key(task.getId(), date))));
            }
        }

        List<WeekResponse.Day> days = new ArrayList<>(7);
        for (var info : calendar.week(start)) {
            List<Occurrence> all = byDay.getOrDefault(info.date(), List.of());
            days.add(new WeekResponse.Day(info,
                    all.stream().filter(o -> o.scheduleMode() == ScheduleMode.DAY).sorted(DAY_TASK_ORDER).toList(),
                    all.stream().filter(o -> o.scheduleMode() == ScheduleMode.FIXED).sorted(TIMED_ORDER).toList()));
        }

        var unscheduled = tasks.findOpenAuto(householdId).stream().map(TaskService::toResponse).toList();
        return new WeekResponse(start, days, unscheduled);
    }

    public PreviewResponse preview(UUID householdId, PreviewRequest request) {
        HebrewCalendar calendar = new HebrewCalendar(access.require(householdId, Permission.READ).household().isInIsrael());
        RecurrenceRule rule = RecurrenceMapper.toRule(request.recurrence());
        int count = request.count() == null ? 5 : request.count();
        LocalDate to = request.from().plusYears(rule instanceof RecurrenceRule.HebrewYearly ? count + 1L : 3L);
        var items = engine.occurrences(rule, request.from(), to).stream()
                .limit(count)
                .map(d -> new PreviewResponse.Item(d, d.getDayOfWeek(), calendar.describe(d).hebrewDate()))
                .toList();
        return new PreviewResponse(items);
    }

    private static void add(Map<LocalDate, List<Occurrence>> byDay, Occurrence o) {
        byDay.computeIfAbsent(o.date(), d -> new ArrayList<>()).add(o);
    }

    private static Occurrence occurrence(Task t, LocalDate date, boolean done) {
        return new Occurrence(t.getId(), t.getTitle(), t.getAssigneeId(), t.getScheduleMode(), date,
                t.getStartTime(), t.getDurationMinutes(), t.getImportance(), t.isRecurring(), done);
    }
}
