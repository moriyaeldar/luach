package io.github.moriyaeldar.luach.tasks.service;

import io.github.moriyaeldar.luach.tasks.api.CompletionRequest;
import io.github.moriyaeldar.luach.tasks.api.RecurrenceDto;
import io.github.moriyaeldar.luach.tasks.api.TaskRequest;
import io.github.moriyaeldar.luach.tasks.api.TaskResponse;
import io.github.moriyaeldar.luach.tasks.domain.Recurrence;
import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;
import io.github.moriyaeldar.luach.tasks.domain.Task;
import io.github.moriyaeldar.luach.tasks.domain.TaskCompletion;
import io.github.moriyaeldar.luach.tasks.domain.TaskCompletionRepository;
import io.github.moriyaeldar.luach.tasks.domain.TaskRepository;
import io.github.moriyaeldar.luach.tasks.domain.TaskStatus;
import io.github.moriyaeldar.luach.tasks.household.HouseholdAccess;
import io.github.moriyaeldar.luach.tasks.household.HouseholdAccess.Access;
import io.github.moriyaeldar.luach.tasks.household.HouseholdAccess.Permission;
import io.github.moriyaeldar.luach.tasks.household.MemberReplica;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final TaskCompletionRepository completions;
    private final HouseholdAccess access;

    public TaskService(TaskRepository tasks, TaskCompletionRepository completions, HouseholdAccess access) {
        this.tasks = tasks;
        this.completions = completions;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID householdId) {
        access.require(householdId, Permission.READ);
        return tasks.findByHouseholdIdOrderByCreatedAtDesc(householdId).stream().map(TaskService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID householdId, UUID id) {
        access.require(householdId, Permission.READ);
        return toResponse(find(householdId, id));
    }

    public TaskResponse create(UUID householdId, TaskRequest request) {
        Access a = access.require(householdId, Permission.EDIT);
        Task task = new Task(householdId);
        apply(task, request, a);
        return toResponse(tasks.save(task));
    }

    public TaskResponse update(UUID householdId, UUID id, TaskRequest request) {
        Access a = access.require(householdId, Permission.EDIT);
        Task task = find(householdId, id);
        apply(task, request, a);
        return toResponse(task);
    }

    public void delete(UUID householdId, UUID id) {
        access.require(householdId, Permission.EDIT);
        tasks.delete(find(householdId, id));
    }

    /** Anyone in the family can tick off a task; children only their own. */
    public TaskResponse setCompletion(UUID householdId, UUID id, CompletionRequest request) {
        Access a = access.require(householdId, Permission.READ);
        Task task = find(householdId, id);
        if (a.isChild() && !task.getAssigneeId().equals(a.member().getId().toString())) {
            throw new HouseholdAccess.ForbiddenException("Children can only tick off their own tasks");
        }
        if (!task.isRecurring()) {
            task.markStatus(request.done() ? TaskStatus.DONE : TaskStatus.OPEN);
            return toResponse(task);
        }
        if (request.date() == null) {
            throw new InvalidTaskException("'date' is required to complete an occurrence of a recurring task");
        }
        var key = new TaskCompletion.Key(task.getId(), request.date());
        if (request.done()) {
            if (!completions.existsById(key)) {
                completions.save(new TaskCompletion(task.getId(), request.date()));
            }
        } else {
            completions.deleteById(key);
        }
        return toResponse(task);
    }

    private Task find(UUID householdId, UUID id) {
        return tasks.findByIdAndHouseholdId(id, householdId).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private static void apply(Task task, TaskRequest r, Access access) {
        validate(r);
        boolean knownMember = access.household().getMembers().stream()
                .map(MemberReplica::getId).anyMatch(id -> id.toString().equals(r.assigneeId()));
        if (!knownMember) {
            throw new InvalidTaskException("The task must be assigned to a member of this family");
        }
        Recurrence recurrence = RecurrenceMapper.toEntity(r.recurrence());
        boolean fixed = r.scheduleMode() == ScheduleMode.FIXED;
        task.update(r.title().strip(), r.notes(), r.assigneeId(), r.scheduleMode(),
                r.scheduleMode() == ScheduleMode.AUTO ? null : r.date(),
                fixed ? r.startTime() : null,
                r.scheduleMode() == ScheduleMode.DAY ? null : r.durationMinutes(),
                r.scheduleMode() == ScheduleMode.AUTO ? r.deadline() : null,
                r.importance() == null ? 3 : r.importance(),
                recurrence);
    }

    /** Business rules that bean validation can't express. */
    static void validate(TaskRequest r) {
        boolean recurring = r.recurrence() != null && r.recurrence().kind() != null
                && r.recurrence().kind() != io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind.NONE;
        switch (r.scheduleMode()) {
            case FIXED -> {
                requirePresent(r.date(), "date", "FIXED");
                requirePresent(r.startTime(), "startTime", "FIXED");
                requirePresent(r.durationMinutes(), "durationMinutes", "FIXED");
            }
            case DAY -> requirePresent(r.date(), "date", "DAY");
            case AUTO -> {
                requirePresent(r.durationMinutes(), "durationMinutes", "AUTO");
                requirePresent(r.deadline(), "deadline", "AUTO");
                if (recurring) {
                    throw new InvalidTaskException("Auto-scheduled tasks can't repeat yet");
                }
            }
        }
        if (recurring && r.recurrence().until() != null && r.date() != null
                && r.recurrence().until().isBefore(r.date())) {
            throw new InvalidTaskException("'until' must not be before the task's date");
        }
    }

    private static void requirePresent(Object value, String field, String mode) {
        if (value == null) {
            throw new InvalidTaskException("'" + field + "' is required for " + mode + " tasks");
        }
    }

    static TaskResponse toResponse(Task t) {
        RecurrenceDto recurrence = RecurrenceMapper.toDto(t.getRecurrence());
        return new TaskResponse(t.getId(), t.getTitle(), t.getNotes(), t.getAssigneeId(), t.getScheduleMode(),
                t.getDate(), t.getStartTime(), t.getDurationMinutes(), t.getDeadline(), t.getImportance(),
                t.getStatus(), recurrence, t.getVersion());
    }
}
