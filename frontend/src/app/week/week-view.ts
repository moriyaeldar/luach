import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { I18n } from '../core/i18n';
import { Member, Occurrence, WeekDay } from '../core/models';
import { endTime, shortTime } from '../core/dates';

/** Seven day columns. Each has a header (Gregorian + Hebrew date, holidays), day tasks and timed tasks. */
@Component({
  selector: 'luach-week-view',
  imports: [MatCheckboxModule, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './week-view.html',
  styleUrl: './week-view.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeekView {
  protected readonly i18n = inject(I18n);

  readonly days = input.required<WeekDay[]>();
  readonly members = input.required<Member[]>();
  readonly today = input.required<string>();

  readonly toggleDone = output<Occurrence>();
  readonly openTask = output<string>();
  readonly addOnDay = output<string>();

  private readonly membersById = computed(() => new Map(this.members().map((m) => [m.id, m])));

  protected member(id: string): Member | undefined {
    return this.membersById().get(id);
  }

  protected memberName(id: string): string {
    const m = this.member(id);
    return m ? this.i18n.pick(m) : id;
  }

  protected timeRange(o: Occurrence): string {
    const end = endTime(o.startTime, o.durationMinutes);
    return end ? `${shortTime(o.startTime)}–${end}` : shortTime(o.startTime);
  }
}
