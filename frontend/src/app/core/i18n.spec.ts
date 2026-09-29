import { TestBed } from '@angular/core/testing';
import { I18n } from './i18n';

describe('I18n', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  it('starts in Hebrew, right to left', () => {
    const i18n = TestBed.inject(I18n);
    TestBed.tick();
    expect(i18n.lang()).toBe('he');
    expect(document.documentElement.dir).toBe('rtl');
    expect(i18n.t('dayTasks')).toBe('משימות היום');
  });

  it('switches to English and left to right, and remembers it', () => {
    const i18n = TestBed.inject(I18n);
    i18n.toggle();
    TestBed.tick();
    expect(i18n.lang()).toBe('en');
    expect(document.documentElement.dir).toBe('ltr');
    expect(document.documentElement.lang).toBe('en');
    expect(i18n.t('dayTasks')).toBe('Day tasks');
    expect(localStorage.getItem('luach.lang')).toBe('en');
  });

  it('picks the text for the current language', () => {
    const i18n = TestBed.inject(I18n);
    expect(i18n.pick({ he: 'שבת', en: 'Shabbat' })).toBe('שבת');
    i18n.toggle();
    expect(i18n.pick({ he: 'שבת', en: 'Shabbat' })).toBe('Shabbat');
  });
});
