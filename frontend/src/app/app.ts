import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterOutlet } from '@angular/router';
import { I18n } from './core/i18n';
import { Session } from './core/session';
import { FamilyDialog } from './family/family-dialog';

/** Shell: toolbar with family switcher, family settings, language and account menu. */
@Component({
  selector: 'app-root',
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly i18n = inject(I18n);
  protected readonly session = inject(Session);
  private readonly dialog = inject(MatDialog);

  protected openFamily(): void {
    this.dialog.open(FamilyDialog, { direction: this.i18n.dir(), maxWidth: '95vw', autoFocus: false });
  }

  protected initial(): string {
    const user = this.session.me()?.user;
    return (user?.name || user?.email || '?').trim().charAt(0).toUpperCase();
  }
}
