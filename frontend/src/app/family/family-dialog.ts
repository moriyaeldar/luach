import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable } from 'rxjs';
import { Api } from '../core/api';
import { I18n, I18nKey } from '../core/i18n';
import { Session } from '../core/session';
import { MEMBER_COLORS, Member, Role } from '../core/models';

interface InviteLink {
  forName: string;
  url: string;
}

/** Members, roles, colors and invite links. Everyone can look; only admins change things. */
@Component({
  selector: 'luach-family-dialog',
  imports: [
    FormsModule, ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule,
    MatIconModule, MatInputModule, MatSelectModule, MatTooltipModule,
  ],
  templateUrl: './family-dialog.html',
  styleUrl: './family-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FamilyDialog {
  protected readonly i18n = inject(I18n);
  protected readonly session = inject(Session);
  private readonly api = inject(Api);

  protected readonly roles: Role[] = ['ADMIN', 'MEMBER', 'CHILD'];
  protected readonly colors = MEMBER_COLORS;
  protected readonly error = signal<string | null>(null);
  protected readonly invite = signal<InviteLink | null>(null);
  protected readonly copied = signal(false);

  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly details = this.fb.group({
    name: [this.session.household()?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    inIsrael: [this.session.household()?.inIsrael ?? true],
  });
  protected readonly newMember = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(60)]],
    role: ['CHILD' as Role],
  });

  protected roleName(role: Role): string {
    return this.i18n.t(('role' + role) as I18nKey);
  }

  protected saveDetails(): void {
    const { name, inIsrael } = this.details.getRawValue();
    this.run(this.api.updateHousehold(this.householdId(), name.trim(), inIsrael), () => this.session.refresh().subscribe());
  }

  protected addMember(): void {
    const { displayName, role } = this.newMember.getRawValue();
    const used = new Set(this.session.members().map((m) => m.color));
    const color = this.colors.find((c) => !used.has(c)) ?? this.colors[0];
    this.run(this.api.addMember(this.householdId(), displayName.trim(), role, color), () => {
      this.newMember.reset({ displayName: '', role: 'CHILD' });
      this.session.membersResource.reload();
    });
  }

  protected changeRole(member: Member, role: Role): void {
    this.run(this.api.updateMember(this.householdId(), member.id, { role }), () => this.reloadAll());
  }

  protected changeColor(member: Member, color: string): void {
    this.run(this.api.updateMember(this.householdId(), member.id, { color }), () => this.session.membersResource.reload());
  }

  protected remove(member: Member): void {
    this.run(this.api.removeMember(this.householdId(), member.id), () => this.session.membersResource.reload());
  }

  protected inviteMember(member: Member): void {
    this.run(this.api.createInvite(this.householdId(), { memberId: member.id }), (r) =>
      this.showInvite(member.displayName, r.token));
  }

  protected inviteNew(): void {
    const { displayName, role } = this.newMember.getRawValue();
    if (!displayName.trim()) {
      this.newMember.controls.displayName.markAsTouched();
      return;
    }
    this.run(this.api.createInvite(this.householdId(), { displayName: displayName.trim(), role }), (r) => {
      this.showInvite(displayName.trim(), r.token);
      this.newMember.reset({ displayName: '', role: 'CHILD' });
    });
  }

  protected copy(url: string): void {
    navigator.clipboard?.writeText(url).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  protected whatsAppUrl(url: string): string {
    return `https://wa.me/?text=${encodeURIComponent(`${this.i18n.t('whatsAppText')} ${url}`)}`;
  }

  private showInvite(forName: string, token: string): void {
    this.copied.set(false);
    this.invite.set({ forName, url: `${location.origin}/join/${token}` });
  }

  private reloadAll(): void {
    this.session.membersResource.reload();
    this.session.refresh().subscribe();
  }

  private householdId(): string {
    return this.session.householdId()!;
  }

  private run<T>(call: Observable<T>, onSuccess: (value: T) => void): void {
    this.error.set(null);
    call.subscribe({
      next: onSuccess,
      error: (e) => this.error.set(e?.error?.detail ?? this.i18n.t('actionFailed')),
    });
  }
}
