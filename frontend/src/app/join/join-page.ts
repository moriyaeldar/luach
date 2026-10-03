import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { Api } from '../core/api';
import { I18n, I18nKey } from '../core/i18n';
import { Session } from '../core/session';

/** /join/:token — shows who invited you, asks you to sign in if needed, and joins the family. */
@Component({
  selector: 'luach-join-page',
  imports: [MatButtonModule, MatProgressBarModule, RouterLink],
  template: `
    <section class="card">
      @if (invite.isLoading() || session.me() === undefined) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (!invite.value()) {
        <p>{{ i18n.t('inviteNotFound') }}</p>
        <a mat-stroked-button routerLink="/">{{ i18n.t('backHome') }}</a>
      } @else {
        @let details = invite.value()!;
        <h1>{{ i18n.t('joinTitle') }}{{ details.householdName }}</h1>
        @if (details.displayName) {
          <p class="as">{{ i18n.t('joinAs') }} <strong>{{ details.displayName }}</strong> · {{ roleName() }}</p>
        }
        @switch (details.status) {
          @case ('USED') {
            <p class="error">{{ i18n.t('inviteUsed') }}</p>
          }
          @case ('EXPIRED') {
            <p class="error">{{ i18n.t('inviteExpired') }}</p>
          }
          @default {
            @if (session.loggedIn() && !session.me()?.user?.demo) {
              <button mat-flat-button (click)="join()" [disabled]="busy()">{{ i18n.t('joinButton') }}</button>
            } @else if (session.config().googleEnabled) {
              <p>{{ i18n.t('joinSignInFirst') }}</p>
              <a mat-flat-button [href]="session.config().googleLoginUrl" (click)="rememberInvite()">
                {{ i18n.t('signInWithGoogle') }}
              </a>
            } @else {
              <p class="error">{{ i18n.t('googleNotConfigured') }}</p>
            }
          }
        }
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      }
    </section>
  `,
  styles: `
    .card {
      max-width: 480px;
      margin: 48px auto;
      padding: 24px;
      text-align: center;
      background: var(--luach-surface);
      border: 1px solid var(--luach-border);
      border-radius: 16px;
    }
    h1 { font-size: 22px; }
    .as { color: var(--luach-muted); }
    .error { color: var(--mat-sys-error); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JoinPage {
  readonly token = input.required<string>();

  protected readonly i18n = inject(I18n);
  protected readonly session = inject(Session);
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly invite = rxResource({
    params: () => this.token(),
    stream: ({ params }) => this.api.invite(params).pipe(catchError(() => of(null))),
  });
  protected readonly roleName = computed(() => {
    const role = this.invite.value()?.role;
    return role ? this.i18n.t(('role' + role) as I18nKey) : '';
  });

  protected rememberInvite(): void {
    this.session.rememberInvite(this.token());
  }

  protected join(): void {
    this.busy.set(true);
    this.api.acceptInvite(this.token()).subscribe({
      next: (household) => {
        this.session.select(household.id);
        this.session.refresh().subscribe(() => this.router.navigate(['/']));
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.detail ?? this.i18n.t('actionFailed'));
      },
    });
  }
}
