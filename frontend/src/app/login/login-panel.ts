import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { I18n } from '../core/i18n';
import { Session } from '../core/session';

/** Shown when nobody is logged in: Google sign-in, or a private demo family without signing up. */
@Component({
  selector: 'luach-login-panel',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <section class="hero">
      <div class="logo" aria-hidden="true">לוח</div>
      <h1>{{ i18n.t('heroTitle') }}</h1>
      <p class="lead">{{ i18n.t('heroText') }}</p>

      @if (failed) {
        <p class="error" role="alert">{{ i18n.t('loginFailed') }}</p>
      }

      <div class="actions">
        @if (session.config().googleEnabled) {
          <a mat-flat-button class="google" [href]="session.config().googleLoginUrl">
            <svg viewBox="0 0 48 48" width="18" height="18" aria-hidden="true">
              <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z"/>
              <path fill="#FF3D00" d="m6.3 14.7 6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/>
              <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z"/>
              <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z"/>
            </svg>
            {{ i18n.t('signInWithGoogle') }}
          </a>
        }
        <button mat-stroked-button (click)="demo()" [disabled]="busy()">
          @if (busy()) {
            <mat-spinner diameter="18" />
          } @else {
            <mat-icon>science</mat-icon>
          }
          {{ i18n.t('tryDemo') }}
        </button>
      </div>
      <p class="hint">{{ i18n.t('tryDemoHint') }}</p>
    </section>
  `,
  styles: `
    .hero {
      max-width: 560px;
      margin: 48px auto;
      padding: 32px 24px;
      text-align: center;
      background: var(--luach-surface);
      border: 1px solid var(--luach-border);
      border-radius: 16px;
    }
    .logo {
      display: inline-grid;
      place-items: center;
      width: 64px;
      height: 64px;
      border-radius: 16px;
      background: var(--mat-sys-primary);
      color: var(--mat-sys-on-primary);
      font-weight: 700;
      font-size: 24px;
    }
    h1 { font-size: 26px; margin: 16px 0 8px; }
    .lead { color: var(--luach-muted); line-height: 1.5; margin: 0 0 24px; }
    .actions { display: flex; flex-wrap: wrap; gap: 12px; justify-content: center; }
    .google svg { margin-inline-end: 8px; background: #fff; border-radius: 50%; padding: 2px; }
    .hint { font-size: 13px; color: var(--luach-muted); margin-top: 12px; }
    .error { color: var(--mat-sys-error); }
    mat-spinner { display: inline-block; margin-inline-end: 8px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPanel {
  protected readonly i18n = inject(I18n);
  protected readonly session = inject(Session);
  protected readonly busy = signal(false);
  protected readonly failed = new URLSearchParams(location.search).get('login') === 'failed';

  protected demo(): void {
    this.busy.set(true);
    this.session.startDemo().subscribe({ error: () => this.busy.set(false) });
  }
}
