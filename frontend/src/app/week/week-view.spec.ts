import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WeekView } from './week-view';
import { Member, Occurrence, WeekDay } from '../core/models';
import { addDays } from '../core/dates';

const members: Member[] = [
  { id: 'ima', displayName: 'Mom', color: '#7c3aed', role: 'ADMIN', connected: true, email: null },
];

function occurrence(partial: Partial<Occurrence>): Occurrence {
  return {
    taskId: 't1', title: 'Task', assigneeId: 'ima', scheduleMode: 'DAY', date: '2026-09-27', startTime: null,
    durationMinutes: null, importance: 3, recurring: false, done: false, ...partial,
  };
}

function week(): WeekDay[] {
  return Array.from({ length: 7 }, (_, i) => {
    const date = addDays('2026-09-27', i);
    return {
      info: {
        date, hebrewYear: 5787, hebrewMonth: 'TISHREI', hebrewDay: 16 + i,
        hebrewDate: { he: `תשרי ${16 + i}`, en: `${16 + i} Tishrei 5787` },
        holidays: i === 6 ? [{ he: 'שמיני עצרת', en: 'Shemini Atzeret' }] : [],
        parasha: null, shabbat: i === 6, restDay: i === 6,
      },
      dayTasks: i === 1 ? [occurrence({ taskId: 'plumber', title: 'Call the plumber', date })] : [],
      timedTasks: i === 2
        ? [occurrence({ taskId: 'dentist', title: 'Dentist', date, scheduleMode: 'FIXED', startTime: '16:00:00', durationMinutes: 45 })]
        : [],
    };
  });
}

describe('WeekView', () => {
  let fixture: ComponentFixture<WeekView>;
  let el: HTMLElement;

  beforeEach(async () => {
    localStorage.setItem('luach.lang', 'en');
    fixture = TestBed.createComponent(WeekView);
    fixture.componentRef.setInput('days', week());
    fixture.componentRef.setInput('members', members);
    fixture.componentRef.setInput('today', '2026-09-29');
    await fixture.whenStable();
    el = fixture.nativeElement;
  });

  it('renders seven days with Hebrew dates and holidays', () => {
    const days = el.querySelectorAll('.day');
    expect(days.length).toBe(7);
    expect(days[0].textContent).toContain('16 Tishrei 5787');
    expect(days[6].textContent).toContain('Shemini Atzeret');
    expect(days[6].classList).toContain('rest');
    expect(days[2].classList).toContain('today');
  });

  it('shows day tasks in their own area and timed tasks with a time range', () => {
    const monday = el.querySelectorAll('.day')[1];
    expect(monday.querySelector('.day-item')?.textContent).toContain('Call the plumber');
    expect(monday.querySelector('.day-item')?.textContent).toContain('Mom');
    const tuesday = el.querySelectorAll('.day')[2];
    expect(tuesday.querySelector('.timed-item .time')?.textContent?.trim()).toBe('16:00–16:45');
  });

  it('emits when a day task is ticked or opened', () => {
    const toggled: Occurrence[] = [];
    const opened: string[] = [];
    fixture.componentInstance.toggleDone.subscribe((o) => toggled.push(o));
    fixture.componentInstance.openTask.subscribe((id) => opened.push(id));

    (el.querySelector('.day-item input[type=checkbox]') as HTMLInputElement).click();
    (el.querySelector('.day-item .item-body') as HTMLButtonElement).click();

    expect(toggled.map((o) => o.taskId)).toEqual(['plumber']);
    expect(opened).toEqual(['plumber']);
  });
});
