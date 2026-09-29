package io.github.moriyaeldar.luach.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, TaskCompletion.Key> {

    @Query("""
            select c from TaskCompletion c
            where c.key.taskId in :taskIds and c.key.occurrenceDate between :from and :to
            """)
    List<TaskCompletion> findForTasksBetween(@Param("taskIds") Collection<java.util.UUID> taskIds,
                                             @Param("from") LocalDate from, @Param("to") LocalDate to);
}
