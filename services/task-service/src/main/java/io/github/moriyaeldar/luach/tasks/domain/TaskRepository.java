package io.github.moriyaeldar.luach.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndHouseholdId(UUID id, UUID householdId);

    List<Task> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);

    /** One-off FIXED and DAY tasks dated inside the range. */
    @Query("""
            select t from Task t
            where t.householdId = :household
              and t.recurrence.kind = io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind.NONE
              and t.scheduleMode <> io.github.moriyaeldar.luach.tasks.domain.ScheduleMode.AUTO
              and t.date between :from and :to
            """)
    List<Task> findOneOffBetween(@Param("household") UUID household, @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    /** Recurring tasks that may have an occurrence inside the range. */
    @Query("""
            select t from Task t
            where t.householdId = :household
              and t.recurrence.kind <> io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind.NONE
              and t.date <= :to
              and (t.recurrence.until is null or t.recurrence.until >= :from)
            """)
    List<Task> findRecurringActiveBetween(@Param("household") UUID household, @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /** AUTO tasks still waiting for the scheduler. */
    @Query("""
            select t from Task t
            where t.householdId = :household
              and t.scheduleMode = io.github.moriyaeldar.luach.tasks.domain.ScheduleMode.AUTO
              and t.status = io.github.moriyaeldar.luach.tasks.domain.TaskStatus.OPEN
            order by t.deadline, t.importance desc
            """)
    List<Task> findOpenAuto(@Param("household") UUID household);
}
