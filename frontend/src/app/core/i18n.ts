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

  // Login and families
  signInWithGoogle: { he: 'כניסה עם Google', en: 'Sign in with Google' },
  tryDemo: { he: 'לנסות את הדמו', en: 'Try the demo' },
  tryDemoHint: { he: 'בלי הרשמה. משפחה לדוגמה משלך, נמחקת אחרי 24 שעות.', en: 'No sign-up. Your own sample family, deleted after 24 hours.' },
  heroTitle: { he: 'כל המשפחה בלוח שבועי אחד', en: 'The whole family in one weekly schedule' },
  heroText: {
    he: 'משימות לכל בן משפחה, תאריכים עבריים ולועזיים, ימי הולדת שחוזרים לפי התאריך העברי וחגים במקום.',
    en: 'Tasks for every family member, Hebrew and Gregorian dates, birthdays that repeat on the Hebrew date, and every holiday in place.',
  },
  loginFailed: { he: 'הכניסה לא הצליחה. נסו שוב.', en: "Sign-in didn't work. Please try again." },
  googleNotConfigured: { he: 'הכניסה עם Google עוד לא מוגדרת בשרת הזה', en: "Google sign-in isn't set up on this server yet" },
  logout: { he: 'יציאה', en: 'Sign out' },
  demoBanner: {
    he: 'זו משפחת דמו: מה שתשנו נשמר רק לכם ונמחק אחרי 24 שעות.',
    en: 'This is a demo family: your changes are private and deleted after 24 hours.',
  },
  createFamilyTitle: { he: 'בואו ניצור את המשפחה שלכם', en: "Let's create your family" },
  createFamilyText: {
    he: 'אחרי זה תוכלו להוסיף את בני המשפחה ולהזמין אותם בקישור.',
    en: 'Then add your family members and invite them with a link.',
  },
  familyName: { he: 'שם המשפחה', en: 'Family name' },
  familyNamePlaceholder: { he: 'למשל: משפחת כהן', en: 'e.g. The Cohens' },
  inIsrael: { he: 'אנחנו בארץ (יום טוב אחד)', en: 'We live in Israel (one day of Yom Tov)' },
  create: { he: 'יצירה', en: 'Create' },
  haveInvite: { he: 'קיבלתם קישור הזמנה? פשוט פתחו אותו.', en: 'Got an invite link? Just open it.' },
  family: { he: 'המשפחה', en: 'Family' },
  switchFamily: { he: 'החלפת משפחה', en: 'Switch family' },

  // Family dialog
  familySettings: { he: 'ניהול המשפחה', en: 'Manage family' },
  members: { he: 'בני המשפחה', en: 'Family members' },
  roleADMIN: { he: 'מנהל/ת', en: 'Admin' },
  roleMEMBER: { he: 'בן/בת משפחה', en: 'Member' },
  roleCHILD: { he: 'ילד/ה', en: 'Child' },
  roleHint: {
    he: 'מנהלים מנהלים את המשפחה. בני משפחה עורכים משימות. ילדים מסמנים את המשימות שלהם.',
    en: 'Admins manage the family. Members edit tasks. Children tick off their own tasks.',
  },
  connected: { he: 'מחובר/ת', en: 'Connected' },
  notConnected: { he: 'בלי חשבון', en: 'No account' },
  invite: { he: 'הזמנה', en: 'Invite' },
  inviteNew: { he: 'הזמנת מישהו חדש', en: 'Invite someone new' },
  addMember: { he: 'הוספת בן משפחה', en: 'Add a member' },
  addMemberHint: { he: 'אפשר להוסיף גם ילדים בלי חשבון, ולהזמין אחר כך.', en: 'You can add children without an account and invite later.' },
  name: { he: 'שם', en: 'Name' },
  role: { he: 'תפקיד', en: 'Role' },
  add: { he: 'הוספה', en: 'Add' },
  remove: { he: 'הסרה', en: 'Remove' },
  inviteLinkFor: { he: 'קישור הזמנה עבור', en: 'Invite link for' },
  inviteLinkHint: { he: 'הקישור תקף לשבוע ולשימוש אחד.', en: 'The link works once, for a week.' },
  copy: { he: 'העתקה', en: 'Copy' },
  copied: { he: 'הועתק', en: 'Copied' },
  shareWhatsApp: { he: 'שליחה בוואטסאפ', en: 'Send on WhatsApp' },
  whatsAppText: { he: 'הצטרפו למשפחה שלנו בלוח:', en: 'Join our family on Luach:' },
  familyDetails: { he: 'פרטי המשפחה', en: 'Family details' },
  close: { he: 'סגירה', en: 'Close' },
  actionFailed: { he: 'הפעולה לא הצליחה', en: "That didn't work" },

  // Join page
  joinTitle: { he: 'הוזמנתם להצטרף ל', en: "You're invited to join " },
  joinAs: { he: 'בתור', en: 'as' },
  joinButton: { he: 'הצטרפות', en: 'Join' },
  joinSignInFirst: { he: 'כדי להצטרף צריך קודם להיכנס עם Google.', en: 'Sign in with Google first to join.' },
  inviteUsed: { he: 'הקישור הזה כבר שומש. בקשו קישור חדש.', en: 'This link was already used. Ask for a new one.' },
  inviteExpired: { he: 'תוקף הקישור פג. בקשו קישור חדש.', en: 'This link has expired. Ask for a new one.' },
  inviteNotFound: { he: 'לא מצאנו את ההזמנה הזו.', en: "We couldn't find this invite." },
  backHome: { he: 'ללוח', en: 'Go to the schedule' },
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
