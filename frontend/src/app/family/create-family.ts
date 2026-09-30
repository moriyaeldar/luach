import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../core/api';
import { I18n } from '../core/i18n';
import { Session } from '../core/session';

/** First step after signing in without an invite: create your family. */
@Component({
  selector: 'luach-create-family',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="card">
      <h1>{{ i18n.t('createFamilyTitle') }}</h1>
      <p class="lead">{{ i18n.t('createFamilyText') }}</p>
      <form [formGroup]="form" (ngSubmit)="create()">
        <mat-form-field appearance="outline" class="full">
          <mat-label>{{ i18n.t('familyName') }}</mat-label>
          <input matInput formControlName="name" [placeholder]="i18n.t('familyNamePlaceholder')" autocomplete="off" />
        </mat-form-field>
        <mat-checkbox formControlName="inIsrael">{{ i18n.t('inIsrael') }}</mat-checkbox>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        <div class="actions">
          <button mat-flat-button type="submit" [disabled]="form.invalid || busy()">{{ i18n.t('create') }}</button>
        </div>
      </form>
      <p class="hint">{{ i18n.t('haveInvite') }}</p>
    </section>
  `,
  styles: `
    .card {
      max-width: 480px;
      margin: 48px auto;
      padding: 24px;
      background: var(--luach-surface);
      border: 1px solid var(--luach-border);
      border-radius: 16px;
    }
    h1 { font-size: 22px; margin: 0 0 8px; }
    .lead, .hint { color: var(--luach-muted); }
    .full { width: 100%; margin-top: 12px; }
    .actions { margin-top: 16px; }
    .error { color: var(--mat-sys-error); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateFamily {
  protected readonly i18n = inject(I18n);
  private readonly api = inject(Api);
  private readonly session = inject(Session);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    inIsrael: [true],
  });

  protected create(): void {
    const { name, inIsrael } = this.form.getRawValue();
    this.busy.set(true);
    this.api.createHousehold(name.trim(), inIsrael).subscribe({
      next: (household) => {
        this.session.select(household.id);
        this.session.refresh().subscribe();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.detail ?? this.i18n.t('actionFailed'));
      },
    });
  }
}
