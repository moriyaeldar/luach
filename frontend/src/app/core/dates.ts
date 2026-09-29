/**
 * Date helpers that work on plain ISO dates ("2026-10-04") so no time zone can shift a day.
 */

export function toIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function parseIsoDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(iso: string, days: number): string {
  const date = parseIsoDate(iso);
  date.setDate(date.getDate() + days);
  return toIsoDate(date);
}

/** The Sunday on or before the given date (the Israeli week starts on Sunday). */
export function startOfWeek(iso: string): string {
  const date = parseIsoDate(iso);
  return addDays(iso, -date.getDay());
}

/** "16:00:00" -> "16:00" */
export function shortTime(time: string | null | undefined): string {
  return time ? time.slice(0, 5) : '';
}

/** "16:00", 45 -> "16:45" */
export function endTime(time: string | null, minutes: number | null): string {
  if (!time || !minutes) {
    return '';
  }
  const [h, m] = time.split(':').map(Number);
  const total = h * 60 + m + minutes;
  return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}
