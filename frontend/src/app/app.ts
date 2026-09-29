import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, of } from 'rxjs';
import { Api } from './core/api';
import { I18n } from './core/i18n';
import { Member, Occurrence, Task, WeekDay } from './core/models';
import { addDays, startOfWeek, toIsoDate } from './core/dates';
import { WeekView } from './week/week-view';
import { TaskDialog, TaskDialogData, TaskDialogResult } from './task-dialog/task-dialog';

@Component({
  selector: 'app-root',
  imports: [
    MatToolbarModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressBarModule, MatTooltipModule,
    WeekView,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly i18n = inject(I18n);
  private readonly api = inject(Api);
  private readonly dialog = inject(MatDialog);

  protected readonly today = toIsoDate(new Date());
  protected readonly weekStart = signal(startOfWeek(this.today));
  protected readonly memberFilter = signal<string | null>(null);

  protected readonly members = toSignal(this.api.members().pipe(catchError(() => of([] as Member[]))), {
    initialValue: [] as Member[],
  });

  protected readonly week = rxResource({
    params: () => this.weekStart(),
    stream: ({ params }) => this.api.week(params),
  });

  /** The week, filtered to one family member when a filter chip is selected. */
  protected readonly days = computed<WeekDay[]>(() => {
    const week = this.week.value();
    if (!week) {
      return [];
    }
    const who = this.memberFilter();
    if (!who) {
      return week.days;
    }
    return week.days.map((d) => ({
      ...d,
      dayTasks: d.dayTasks.filter((t) => t.assigneeId === who),
      timedTasks: d.timedTasks.filter((t) => t.assigneeId === who),
    }));
  });

  protected readonly unscheduled = computed<Task[]>(() => {
    const who = this.memberFilter();
    const tasks = this.week.value()?.unscheduled ?? [];
    return who ? tasks.filter((t) => t.assigneeId === who) : tasks;
  });

  /** "4–10 Oct 2026 · כ״ג–כ״ט תשרי" style range for the toolbar. */
  protected readonly rangeLabel = computed(() => {
    const start = this.weekStart();
    const end = addDays(start, 6);
    const days = this.week.value()?.days;
    const hebrew = days?.length
      ? `${this.i18n.pick(days[0].info.hebrewDate)} – ${this.i18n.pick(days[6].info.hebrewDate)}`
      : '';
    return { gregorian: `${this.i18n.shortDate(start)} – ${this.i18n.longDate(end)}`, hebrew };
  });

  protected previousWeek(): void {
    this.weekStart.update((s) => addDays(s, -7));
  }

  protected nextWeek(): void {
    this.weekStart.update((s) => addDays(s, 7));
  }

  protected goToToday(): void {
    this.weekStart.set(startOfWeek(this.today));
  }

  protected toggleDone(o: Occurrence): void {
    this.api.setDone(o.taskId, o.date, !o.done).subscribe(() => this.week.reload());
  }

  protected newTask(date?: string): void {
    this.openDialog({ date: date ?? this.today, members: this.members() });
  }

  protected openTask(id: string): void {
    this.api.task(id).subscribe((task) => this.openDialog({ task, members: this.members() }));
  }

  protected memberName(id: string): string {
    const m = this.members().find((x) => x.id === id);
    return m ? this.i18n.pick(m) : id;
  }

  protected memberColor(id: string): string | undefined {
    return this.members().find((x) => x.id === id)?.color;
  }

  private openDialog(data: TaskDialogData): void {
    this.dialog
      .open<TaskDialog, TaskDialogData, TaskDialogResult>(TaskDialog, {
        data,
        direction: this.i18n.dir(),
        autoFocus: 'first-tabbable',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.week.reload();
        }
      });
  }
}
