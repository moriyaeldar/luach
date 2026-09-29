import { addDays, endTime, shortTime, startOfWeek } from './dates';

describe('dates', () => {
  it('finds the Sunday that starts the week', () => {
    expect(startOfWeek('2026-09-29')).toBe('2026-09-27'); // Tuesday -> Sunday
    expect(startOfWeek('2026-09-27')).toBe('2026-09-27'); // Sunday stays
    expect(startOfWeek('2026-10-03')).toBe('2026-09-27'); // Saturday -> previous Sunday
  });

  it('adds days across month and year boundaries', () => {
    expect(addDays('2026-09-30', 1)).toBe('2026-10-01');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('formats times', () => {
    expect(shortTime('16:30:00')).toBe('16:30');
    expect(endTime('16:30', 45)).toBe('17:15');
    expect(endTime('23:30', 60)).toBe('00:30');
    expect(endTime(null, 30)).toBe('');
  });
});
