import { DOCUMENT, Injectable, computed, effect, inject, signal } from '@angular/core';
import { Lang, LocalizedText } from './models';
import { parseIsoDate } from './dates';

const STORAGE_KEY = 'luach.lang';

const DICTIONARY = {
  appName: { he: 'לוח', en: 'Luach' },
  tagline: { he: 'הלוח השבועי של המשפחה', en: 'The family weekly schedule' },
  switchLanguage: { he: 'English', en: 'עברית' },
  today: { he: 'היום', en: 'Today' },
  previousWeek: { he: 'השבוע הקודם', en: 'Previous week' },
  nextWeek: { he: 'השבוע הבא', en: 'Next week' },
  newTask: { he: 'משימה חדשה', en: 'New task' },
  everyone: { he: 'כולם', en: 'Everyone' },
  dayTasks: { he: 'משימות היום', en: 'Day tasks' },
  timedTasks: { he: 'לפי שעה', en: 'Scheduled' },
  nothingPlanned: { he: 'אין משימות', en: 'Nothing planned' },
  parasha: { he: 'פרשת', en: 'Parashat' },
  unscheduled: { he: 'ממתינות לשיבוץ', en: 'Waiting to be scheduled' },
  unscheduledHint: {
    he: 'משימות עם משך ודדליין. השיבוץ האוטומטי לחלון פנוי יתווסף בשלב הבא.',
    en: 'Tasks with a duration and a deadline. Automatic placement in a free slot comes next.',
  },
  deadline: { he: 'עד', en: 'By' },
  loadError: { he: 'לא הצלחנו לטעון את הלוח. נסו לרענן.', en: "Couldn't load the schedule. Try refreshing." },
  recurring: { he: 'חוזרת', en: 'Repeats' },
  markDone: { he: 'סימון כבוצע', en: 'Mark as done' },

  // Task dialog
  editTask: { he: 'עריכת משימה', en: 'Edit task' },
  title: { he: 'מה צריך לעשות?', en: 'What needs doing?' },
  notes: { he: 'הערות', en: 'Notes' },
  assignee: { he: 'באחריות', en: 'Assigned to' },
  kind: { he: 'סוג', en: 'Type' },
  modeFixed: { he: 'בשעה קבועה', en: 'At a set time' },
  modeDay: { he: 'משימת יום', en: 'Day task' },
  modeAuto: { he: 'שיבוץ אוטומטי', en: 'Auto-schedule' },
  modeFixedHint: { he: 'למשל: רופא שיניים ביום שלישי ב־16:00', en: 'e.g. dentist on Tuesday at 16:00' },
  modeDayHint: { he: 'ביום מסוים, בלי שעה. למשל: להתקשר לאינסטלטור', en: 'On a day, no time. e.g. call the plumber' },
  modeAutoHint: { he: 'משך ודדליין, המערכת תמצא זמן פנוי', en: 'A duration and a deadline; Luach finds the time' },
  date: { he: 'תאריך', en: 'Date' },
  firstDate: { he: 'החל מתאריך', en: 'Starting from' },
  startTime: { he: 'שעה', en: 'Time' },
  duration: { he: 'משך (דקות)', en: 'Duration (minutes)' },
  deadlineField: { he: 'דדליין', en: 'Deadline' },
  importance: { he: 'חשיבות', en: 'Importance' },
  repeat: { he: 'חזרה', en: 'Repeat' },
  repeatNone: { he: 'לא חוזרת', en: "Doesn't repeat" },
  repeatHebrewYearly: { he: 'כל שנה בתאריך עברי', en: 'Every year on a Hebrew date' },
  repeatHebrewMonthly: { he: 'כל חודש עברי', en: 'Every Hebrew month' },
  repeatRoshChodesh: { he: 'בכל ראש חודש', en: 'Every Rosh Chodesh' },
  repeatWeekly: { he: 'כל שבוע', en: 'Every week' },
  repeatGregorianMonthly: { he: 'כל חודש לועזי', en: 'Every Gregorian month' },
  hebrewMonth: { he: 'חודש עברי', en: 'Hebrew month' },
  hebrewDay: { he: 'יום בחודש', en: 'Day of month' },
  leapYearPolicy: { he: 'בשנה מעוברת', en: 'In a leap year' },
  leapAdarII: { he: 'באדר ב׳', en: 'In Adar II' },
  leapAdarI: { he: 'באדר א׳', en: 'In Adar I' },
  missingDayPolicy: { he: 'כשהיום לא קיים (למשל ל׳ חשוון)', en: "When the day doesn't exist (e.g. 30 Cheshvan)" },
  missingNextDay: { he: 'ביום שאחרי (א׳ בחודש הבא)', en: 'The next day (1st of next month)' },
  missingLastDay: { he: 'ביום האחרון בחודש (כ״ט)', en: 'The last day of the month (29th)' },
  missingSkip: { he: 'לדלג', en: 'Skip' },
  weekDays: { he: 'ימים', en: 'Days' },
  monthDay: { he: 'יום בחודש', en: 'Day of month' },
  until: { he: 'עד תאריך (לא חובה)', en: 'Until (optional)' },
  preview: { he: 'המופעים הבאים', en: 'Next occurrences' },
  previewEmpty: { he: 'אין מופעים בטווח', en: 'No occurrences in range' },
  save: { he: 'שמירה', en: 'Save' },
  cancel: { he: 'ביטול', en: 'Cancel' },
  delete: { he: 'מחיקה', en: 'Delete' },
  required: { he: 'שדה חובה', en: 'Required' },
  saveError: { he: 'השמירה נכשלה', en: "Couldn't save" },
} satisfies Record<string, LocalizedText>;

export type I18nKey = keyof typeof DICTIONARY;

/** Runtime language switching (Hebrew / English) including page direction. */
@Injectable({ providedIn: 'root' })
export class I18n {
  private readonly document = inject(DOCUMENT);

  readonly lang = signal<Lang>(this.initialLang());
  readonly dir = computed(() => (this.lang() === 'he' ? 'rtl' : 'ltr'));
  readonly locale = computed(() => (this.lang() === 'he' ? 'he-IL' : 'en-GB'));

  constructor() {
    effect(() => {
      const lang = this.lang();
      const root = this.document.documentElement;
      root.lang = lang;
      root.dir = this.dir();
      try {
        localStorage.setItem(STORAGE_KEY, lang);
      } catch {
        // Storage can be unavailable (private mode); the language still works for this visit.
      }
    });
  }

  toggle(): void {
    this.lang.update((l) => (l === 'he' ? 'en' : 'he'));
  }

  t(key: I18nKey): string {
    return DICTIONARY[key][this.lang()];
  }

  pick(text: LocalizedText | null | undefined): string {
    return text ? text[this.lang()] : '';
  }

  weekday(iso: string, style: 'long' | 'short' = 'short'): string {
    return new Intl.DateTimeFormat(this.locale(), { weekday: style }).format(parseIsoDate(iso));
  }

  shortDate(iso: string): string {
    return new Intl.DateTimeFormat(this.locale(), { day: 'numeric', month: 'short' }).format(parseIsoDate(iso));
  }

  longDate(iso: string): string {
    return new Intl.DateTimeFormat(this.locale(), { day: 'numeric', month: 'long', year: 'numeric' })
      .format(parseIsoDate(iso));
  }

  private initialLang(): Lang {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === 'he' || saved === 'en') {
        return saved;
      }
    } catch {
      // ignore
    }
    return 'he';
  }
}
