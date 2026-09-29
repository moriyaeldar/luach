package io.github.moriyaeldar.luach.tasks.api;

import io.github.moriyaeldar.luach.calendar.DayInfo;

import java.time.LocalDate;
import java.util.List;

/**
 * The weekly schedule.
 *
 * @param days        seven days, each with its Hebrew-calendar info, day tasks and timed tasks
 * @param unscheduled AUTO tasks waiting for the scheduler (placed automatically from milestone 3)
 */
public record WeekResponse(LocalDate start, List<Day> days, List<TaskResponse> unscheduled) {

    public record Day(DayInfo info, List<Occurrence> dayTasks, List<Occurrence> timedTasks) {
    }
}
