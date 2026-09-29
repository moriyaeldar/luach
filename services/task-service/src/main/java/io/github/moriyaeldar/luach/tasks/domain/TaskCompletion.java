package io.github.moriyaeldar.luach.tasks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One completed occurrence of a recurring task. */
@Entity
@Table(name = "task_completion")
public class TaskCompletion {

    @EmbeddedId
    private Key key;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected TaskCompletion() {
    }

    public TaskCompletion(UUID taskId, LocalDate occurrenceDate) {
        this.key = new Key(taskId, occurrenceDate);
        this.completedAt = Instant.now();
    }

    public Key getKey() {
        return key;
    }

    @Embeddable
    public record Key(@Column(name = "task_id") UUID taskId,
                      @Column(name = "occurrence_date") LocalDate occurrenceDate) implements Serializable {
    }
}
