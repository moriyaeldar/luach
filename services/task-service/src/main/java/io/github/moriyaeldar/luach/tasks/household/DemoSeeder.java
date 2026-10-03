package io.github.moriyaeldar.luach.tasks.household;

import io.github.moriyaeldar.luach.calendar.HebrewMonth;
import io.github.moriyaeldar.luach.tasks.domain.Recurrence;
import io.github.moriyaeldar.luach.tasks.domain.RecurrenceKind;
import io.github.moriyaeldar.luach.tasks.domain.ScheduleMode;
import io.github.moriyaeldar.luach.tasks.domain.Task;
import io.github.moriyaeldar.luach.tasks.domain.TaskRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Fills a new demo family with a realistic week, exactly once (from the Kafka event or on first access, whichever comes
 * first). Members are in the order the Household service creates them: parent (the visitor), partner, two children.
 */
@Component
public class DemoSeeder {

    private final HouseholdReplicaRepository replicas;
    private final TaskRepository tasks;

    public DemoSeeder(HouseholdReplicaRepository replicas, TaskRepository tasks) {
        this.replicas = replicas;
        this.tasks = tasks;
    }

    /** Own transaction: it may be triggered from a read-only request such as the week view. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedOnce(HouseholdReplica household) {
        List<MemberReplica> m = household.getMembers();
        if (m.size() < 4 || replicas.markDemoSeeded(household.getId()) == 0) {
            return;
        }
        UUID h = household.getId();
        String parent = id(m.get(0)), partner = id(m.get(1)), daughter = id(m.get(2)), son = id(m.get(3));
        LocalDate sunday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        timed(h, "אימון כדורגל", son, sunday, "17:00", 90, weekly(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY));
        timed(h, "חוג ציור", daughter, sunday, "16:30", 60, weekly(DayOfWeek.TUESDAY));
        timed(h, "אסיפת הורים", parent, sunday.plusDays(4), "18:00", 45, Recurrence.none());
        day(h, "לסדר את החדר", son, sunday, 3, weekly(DayOfWeek.FRIDAY));
        day(h, "להחזיר ספרים לספרייה", daughter, sunday.plusDays(2), 2, Recurrence.none());
        day(h, "לשלם ארנונה", partner, sunday.plusDays(3), 5, Recurrence.none());
        day(h, "להדליק נר לראש חודש", parent, sunday, 3,
                new Recurrence(RecurrenceKind.ROSH_CHODESH, null, null, null, null, null, null, null));
        day(h, "יום הולדת לסבתא", parent, sunday, 5,
                new Recurrence(RecurrenceKind.HEBREW_YEARLY, HebrewMonth.TEVET, 3, null, null, null, null, null));

        Task storage = new Task(h);
        storage.update("לסדר את המחסן", null, partner, ScheduleMode.AUTO, null, null, 90, sunday.plusDays(5), 3,
                Recurrence.none());
        tasks.save(storage);
    }

    private void timed(UUID h, String title, String who, LocalDate date, String time, int minutes, Recurrence r) {
        Task t = new Task(h);
        t.update(title, null, who, ScheduleMode.FIXED, date, LocalTime.parse(time), minutes, null, 3, r);
        tasks.save(t);
    }

    private void day(UUID h, String title, String who, LocalDate date, int importance, Recurrence r) {
        Task t = new Task(h);
        t.update(title, null, who, ScheduleMode.DAY, date, null, null, null, importance, r);
        tasks.save(t);
    }

    private static Recurrence weekly(DayOfWeek... days) {
        return new Recurrence(RecurrenceKind.WEEKLY, null, null, null, null, EnumSet.of(days[0], days), null, null);
    }

    private static String id(MemberReplica member) {
        return member.getId().toString();
    }
}
