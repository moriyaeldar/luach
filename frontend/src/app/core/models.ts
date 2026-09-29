export type Lang = 'he' | 'en';

export interface LocalizedText {
  he: string;
  en: string;
}

export type ScheduleMode = 'FIXED' | 'DAY' | 'AUTO';
export type RecurrenceKind =
  | 'NONE'
  | 'HEBREW_YEARLY'
  | 'HEBREW_MONTHLY'
  | 'ROSH_CHODESH'
  | 'WEEKLY'
  | 'GREGORIAN_MONTHLY';
export type HebrewMonth =
  | 'TISHREI' | 'CHESHVAN' | 'KISLEV' | 'TEVET' | 'SHEVAT' | 'ADAR' | 'ADAR_I' | 'ADAR_II'
  | 'NISAN' | 'IYAR' | 'SIVAN' | 'TAMMUZ' | 'AV' | 'ELUL';
export type LeapYearPolicy = 'ADAR_II' | 'ADAR_I';
export type MissingDayPolicy = 'NEXT_DAY' | 'LAST_DAY' | 'SKIP';
export type DayOfWeek = 'SUNDAY' | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY';

export interface Recurrence {
  kind: RecurrenceKind;
  hebrewMonth?: HebrewMonth | null;
  hebrewDay?: number | null;
  leapYearPolicy?: LeapYearPolicy | null;
  missingDayPolicy?: MissingDayPolicy | null;
  weekDays?: DayOfWeek[] | null;
  monthDay?: number | null;
  until?: string | null;
}

export interface TaskRequest {
  title: string;
  notes?: string | null;
  assigneeId: string;
  scheduleMode: ScheduleMode;
  date?: string | null;
  startTime?: string | null;
  durationMinutes?: number | null;
  deadline?: string | null;
  importance?: number | null;
  recurrence?: Recurrence | null;
}

export interface Task extends TaskRequest {
  id: string;
  importance: number;
  status: 'OPEN' | 'DONE';
  recurrence: Recurrence;
  version: number;
}

export interface Occurrence {
  taskId: string;
  title: string;
  assigneeId: string;
  scheduleMode: ScheduleMode;
  date: string;
  startTime: string | null;
  durationMinutes: number | null;
  importance: number;
  recurring: boolean;
  done: boolean;
}

export interface DayInfo {
  date: string;
  hebrewYear: number;
  hebrewMonth: HebrewMonth;
  hebrewDay: number;
  hebrewDate: LocalizedText;
  holidays: LocalizedText[];
  parasha: LocalizedText | null;
  shabbat: boolean;
  restDay: boolean;
}

export interface WeekDay {
  info: DayInfo;
  dayTasks: Occurrence[];
  timedTasks: Occurrence[];
}

export interface WeekResponse {
  start: string;
  days: WeekDay[];
  unscheduled: Task[];
}

export interface Member {
  id: string;
  he: string;
  en: string;
  color: string;
}

export interface PreviewItem {
  date: string;
  dayOfWeek: DayOfWeek;
  hebrewDate: LocalizedText;
}

/** Hebrew months in calendar order, starting from Tishrei. */
export const HEBREW_MONTHS: { value: HebrewMonth; name: LocalizedText }[] = [
  { value: 'TISHREI', name: { he: 'תשרי', en: 'Tishrei' } },
  { value: 'CHESHVAN', name: { he: 'חשוון', en: 'Cheshvan' } },
  { value: 'KISLEV', name: { he: 'כסלו', en: 'Kislev' } },
  { value: 'TEVET', name: { he: 'טבת', en: 'Tevet' } },
  { value: 'SHEVAT', name: { he: 'שבט', en: 'Shevat' } },
  { value: 'ADAR', name: { he: 'אדר', en: 'Adar' } },
  { value: 'ADAR_I', name: { he: 'אדר א׳', en: 'Adar I' } },
  { value: 'ADAR_II', name: { he: 'אדר ב׳', en: 'Adar II' } },
  { value: 'NISAN', name: { he: 'ניסן', en: 'Nisan' } },
  { value: 'IYAR', name: { he: 'אייר', en: 'Iyar' } },
  { value: 'SIVAN', name: { he: 'סיוון', en: 'Sivan' } },
  { value: 'TAMMUZ', name: { he: 'תמוז', en: 'Tammuz' } },
  { value: 'AV', name: { he: 'אב', en: 'Av' } },
  { value: 'ELUL', name: { he: 'אלול', en: 'Elul' } },
];

export const WEEK_DAYS: DayOfWeek[] = [
  'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY',
];
