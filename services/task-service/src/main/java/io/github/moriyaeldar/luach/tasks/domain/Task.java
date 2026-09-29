package io.github.moriyaeldar.luach.tasks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "task")
public class Task {

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false)
    private String title;

    private String notes;

    @Column(name = "assignee_id", nullable = false)
    private String assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_mode", nullable = false)
    private ScheduleMode scheduleMode;

    /** FIXED and DAY: the date (the first occurrence for recurring tasks). */
    @Column(name = "task_date")
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** AUTO: the latest day the task may be done. */
    private LocalDate deadline;

    @Column(nullable = false)
    private int importance = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.OPEN;

    @Embedded
    private Recurrence recurrence = Recurrence.none();

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    public Task(UUID householdId) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(String title, String notes, String assigneeId, ScheduleMode scheduleMode, LocalDate date,
                       LocalTime startTime, Integer durationMinutes, LocalDate deadline, int importance,
                       Recurrence recurrence) {
        this.title = title;
        this.notes = notes;
        this.assigneeId = assigneeId;
        this.scheduleMode = scheduleMode;
        this.date = date;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.deadline = deadline;
        this.importance = importance;
        this.recurrence = recurrence == null ? Recurrence.none() : recurrence;
    }

    public void markStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isRecurring() {
        return recurrence != null && recurrence.isRecurring();
    }

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public ScheduleMode getScheduleMode() {
        return scheduleMode;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public int getImportance() {
        return importance;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Recurrence getRecurrence() {
        return recurrence == null ? Recurrence.none() : recurrence;
    }

    public long getVersion() {
        return version;
    }
}
