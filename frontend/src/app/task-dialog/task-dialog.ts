import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { catchError, debounceTime, map, of, startWith, switchMap } from 'rxjs';
import { Api } from '../core/api';
import { I18n, I18nKey } from '../core/i18n';
import {
  DayOfWeek, HEBREW_MONTHS, HebrewMonth, LeapYearPolicy, Member, MissingDayPolicy, PreviewItem, Recurrence,
  RecurrenceKind, ScheduleMode, Task, TaskRequest, WEEK_DAYS,
} from '../core/models';
import { addDays, parseIsoDate, shortTime } from '../core/dates';

export interface TaskDialogData {
  task?: Task;
  date?: string;
  members: Member[];
}

export type TaskDialogResult = 'saved' | 'deleted';

@Component({
  selector: 'luach-task-dialog',
  imports: [
    ReactiveFormsModule, NgTemplateOutlet, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule,
    MatButtonToggleModule, MatIconModule,
  ],
  templateUrl: './task-dialog.html',
  styleUrl: './task-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskDialog {
  protected readonly i18n = inject(I18n);
  protected readonly data = inject<TaskDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(Api);
  private readonly ref = inject<MatDialogRef<TaskDialog, TaskDialogResult>>(MatDialogRef);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly months = HEBREW_MONTHS;
  protected readonly weekDays = WEEK_DAYS;
  protected readonly modes: { value: ScheduleMode; label: I18nKey; hint: I18nKey }[] = [
    { value: 'DAY', label: 'modeDay', hint: 'modeDayHint' },
    { value: 'FIXED', label: 'modeFixed', hint: 'modeFixedHint' },
    { value: 'AUTO', label: 'modeAuto', hint: 'modeAutoHint' },
  ];
  protected readonly kinds: { value: RecurrenceKind; label: I18nKey }[] = [
    { value: 'NONE', label: 'repeatNone' },
    { value: 'HEBREW_YEARLY', label: 'repeatHebrewYearly' },
    { value: 'HEBREW_MONTHLY', label: 'repeatHebrewMonthly' },
    { value: 'ROSH_CHODESH', label: 'repeatRoshChodesh' },
    { value: 'WEEKLY', label: 'repeatWeekly' },
    { value: 'GREGORIAN_MONTHLY', label: 'repeatGregorianMonthly' },
  ];
  protected readonly hebrewDays = Array.from({ length: 30 }, (_, i) => i + 1);

  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly form = this.fb.group({
    title: [this.data.task?.title ?? '', [Validators.required, Validators.maxLength(200)]],
    notes: [this.data.task?.notes ?? ''],
    assigneeId: [this.data.task?.assigneeId ?? this.data.members[0]?.id ?? '', Validators.required],
    scheduleMode: [this.data.task?.scheduleMode ?? ('DAY' as ScheduleMode)],
    date: [this.data.task?.date ?? this.data.date ?? ''],
    startTime: [shortTime(this.data.task?.startTime) || '09:00'],
    durationMinutes: [this.data.task?.durationMinutes ?? 30],
    deadline: [this.data.task?.deadline ?? addDays(this.data.date ?? this.todayIso(), 3)],
    importance: [this.data.task?.importance ?? 3],
    recurrence: this.fb.group({
      kind: [this.data.task?.recurrence?.kind ?? ('NONE' as RecurrenceKind)],
      hebrewMonth: [this.data.task?.recurrence?.hebrewMonth ?? ('TISHREI' as HebrewMonth)],
      hebrewDay: [this.data.task?.recurrence?.hebrewDay ?? 1],
      leapYearPolicy: [this.data.task?.recurrence?.leapYearPolicy ?? ('ADAR_II' as LeapYearPolicy)],
      missingDayPolicy: [this.data.task?.recurrence?.missingDayPolicy ?? ('NEXT_DAY' as MissingDayPolicy)],
      weekDays: [this.data.task?.recurrence?.weekDays ?? ([] as DayOfWeek[])],
      monthDay: [this.data.task?.recurrence?.monthDay ?? 1],
      until: [this.data.task?.recurrence?.until ?? ''],
    }),
  });

  protected readonly mode = toSignal(this.form.controls.scheduleMode.valueChanges, {
    initialValue: this.form.controls.scheduleMode.value,
  });
  protected readonly kind = toSignal(this.form.controls.recurrence.controls.kind.valueChanges, {
    initialValue: this.form.controls.recurrence.controls.kind.value,
  });

  /** Live preview of the next occurrences, so "30 Cheshvan" or "Adar" is never a surprise. */
  protected readonly preview = toSignal(
    this.form.valueChanges.pipe(
      startWith(this.form.value),
      debounceTime(300),
      map(() => this.recurrence()),
      switchMap((recurrence) => {
        const from = this.form.controls.date.value || this.todayIso();
        if (recurrence.kind === 'NONE' || this.form.controls.scheduleMode.value === 'AUTO') {
          return of(null);
        }
        if (recurrence.kind === 'WEEKLY' && !recurrence.weekDays?.length) {
          return of([] as PreviewItem[]);
        }
        return this.api.preview(recurrence, from).pipe(
          map((r) => r.occurrences),
          catchError(() => of([] as PreviewItem[])),
        );
      }),
    ),
    { initialValue: null },
  );

  constructor() {
    this.form.controls.scheduleMode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((mode) => {
        if (mode === 'AUTO') {
          this.form.controls.recurrence.controls.kind.setValue('NONE');
        }
      });
  }

  protected isEdit(): boolean {
    return !!this.data.task;
  }

  protected previewDate(iso: string): string {
    return `${this.i18n.weekday(iso)} ${new Intl.DateTimeFormat(this.i18n.locale(), {
      day: 'numeric', month: 'short', year: 'numeric',
    }).format(parseIsoDate(iso))}`;
  }

  protected dayName(day: DayOfWeek): string {
    // 2026-10-04 is a Sunday; WEEK_DAYS starts on Sunday.
    return this.i18n.weekday(addDays('2026-10-04', WEEK_DAYS.indexOf(day)), 'long');
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const request = this.toRequest();
    const call = this.data.task ? this.api.update(this.data.task.id, request) : this.api.create(request);
    call.subscribe({
      next: () => this.ref.close('saved'),
      error: (e) => {
        this.saving.set(false);
        this.error.set(e?.error?.detail ?? this.i18n.t('saveError'));
      },
    });
  }

  protected remove(): void {
    if (!this.data.task) {
      return;
    }
    this.saving.set(true);
    this.api.remove(this.data.task.id).subscribe({
      next: () => this.ref.close('deleted'),
      error: () => {
        this.saving.set(false);
        this.error.set(this.i18n.t('saveError'));
      },
    });
  }

  private recurrence(): Recurrence {
    const r = this.form.controls.recurrence.getRawValue();
    switch (r.kind) {
      case 'HEBREW_YEARLY':
        return {
          kind: r.kind, hebrewMonth: r.hebrewMonth, hebrewDay: r.hebrewDay, leapYearPolicy: r.leapYearPolicy,
          missingDayPolicy: r.missingDayPolicy, until: r.until || null,
        };
      case 'HEBREW_MONTHLY':
        return { kind: r.kind, hebrewDay: r.hebrewDay, missingDayPolicy: r.missingDayPolicy, until: r.until || null };
      case 'ROSH_CHODESH':
        return { kind: r.kind, until: r.until || null };
      case 'WEEKLY':
        return { kind: r.kind, weekDays: r.weekDays, until: r.until || null };
      case 'GREGORIAN_MONTHLY':
        return { kind: r.kind, monthDay: r.monthDay, until: r.until || null };
      default:
        return { kind: 'NONE' };
    }
  }

  private toRequest(): TaskRequest {
    const v = this.form.getRawValue();
    return {
      title: v.title.trim(),
      notes: v.notes || null,
      assigneeId: v.assigneeId,
      scheduleMode: v.scheduleMode,
      date: v.scheduleMode === 'AUTO' ? null : v.date || null,
      startTime: v.scheduleMode === 'FIXED' ? v.startTime : null,
      durationMinutes: v.scheduleMode === 'DAY' ? null : v.durationMinutes,
      deadline: v.scheduleMode === 'AUTO' ? v.deadline : null,
      importance: v.importance,
      recurrence: this.recurrence(),
    };
  }

  private todayIso(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
