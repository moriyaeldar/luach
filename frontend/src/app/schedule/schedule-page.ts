import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { Api } from '../core/api';
import { I18n } from '../core/i18n';
import { Session } from '../core/session';
import { Occurrence, Task, WeekDay } from '../core/models';
import { addDays, startOfWeek, toIsoDate } from '../core/dates';
import { WeekView } from '../week/week-view';
import { TaskDialog, TaskDialogData, TaskDialogResult } from '../task-dialog/task-dialog';
import { LoginPanel } from '../login/login-panel';
import { CreateFamily } from '../family/create-family';

/** The home page: login, then creating a family, then the weekly schedule. */
@Component({
  selector: 'luach-schedule-page',
  imports: [
    MatButtonModule, MatIconModule, MatChipsModule, MatProgressBarModule, MatTooltipModule, WeekView, LoginPanel,
    CreateFamily,
  ],
  templateUrl: './schedule-page.html',
  styleUrl: './schedule-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchedulePage {
  protected readonly i18n = inject(I18n);
  protected readonly session = inject(Session);
  private readonly api = inject(Api);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  protected readonly today = toIsoDate(new Date());
  protected readonly weekStart = signal(startOfWeek(this.today));
  protected readonly memberFilter = signal<string | null>(null);

  protected readonly week = rxResource({
    params: () => (this.session.householdId() ? { start: this.weekStart(), household: this.session.householdId() } : undefined),
    stream: ({ params }) => this.api.week(params.start),
  });

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

  protected readonly rangeLabel = computed(() => {
    const start = this.weekStart();
    const days = this.week.value()?.days;
    const hebrew = days?.length
      ? `${this.i18n.pick(days[0].info.hebrewDate)} – ${this.i18n.pick(days[6].info.hebrewDate)}`
      : '';
    return { gregorian: `${this.i18n.shortDate(start)} – ${this.i18n.longDate(addDays(start, 6))}`, hebrew };
  });

  constructor() {
    // Someone opened an invite link before logging in: take them back to it.
    effect(() => {
      const token = this.session.pendingInvite();
      if (this.session.loggedIn() && token) {
        this.session.rememberInvite(null);
        this.router.navigate(['/join', token]);
      }
    });
    // A different family means a different set of members to filter by.
    effect(() => {
      this.session.householdId();
      this.memberFilter.set(null);
    });
  }

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
    this.api.setDone(o.taskId, o.date, !o.done).subscribe({ next: () => this.week.reload() });
  }

  protected newTask(date?: string): void {
    if (this.session.canEdit()) {
      this.openDialog({ date: date ?? this.today, members: this.session.members() });
    }
  }

  protected openTask(id: string): void {
    if (this.session.canEdit()) {
      this.api.task(id).subscribe((task) => this.openDialog({ task, members: this.session.members() }));
    }
  }

  protected memberName(id: string): string {
    return this.session.members().find((m) => m.id === id)?.displayName ?? '';
  }

  protected memberColor(id: string): string | undefined {
    return this.session.members().find((m) => m.id === id)?.color;
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
