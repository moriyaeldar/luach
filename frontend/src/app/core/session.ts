import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, catchError, of, tap } from 'rxjs';
import { Api } from './api';
import { AuthConfig, Me, Member, Membership } from './models';
import { RequestContext } from './request-context';

const HOUSEHOLD_KEY = 'luach.household';
const PENDING_INVITE_KEY = 'luach.pendingInvite';

/**
 * Who is logged in, which family they're looking at, and that family's members.
 * The session itself is an HttpOnly cookie the app never sees; {@link me} tells us whether it's valid.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly api = inject(Api);

  /** undefined = still loading, null = not logged in. */
  readonly me = signal<Me | null | undefined>(undefined);
  readonly config = signal<AuthConfig>({ googleEnabled: false, googleLoginUrl: '/oauth2/authorization/google' });
  private readonly selectedId = signal<string | null>(read(HOUSEHOLD_KEY));

  readonly loggedIn = computed(() => !!this.me());
  readonly households = computed(() => this.me()?.households ?? []);
  readonly household = computed<Membership | null>(() => {
    const list = this.households();
    return list.find((h) => h.id === this.selectedId()) ?? list[0] ?? null;
  });
  readonly householdId = computed(() => this.household()?.id ?? null);
  readonly isAdmin = computed(() => this.household()?.role === 'ADMIN');
  /** Children see the schedule and tick off their own tasks, but don't edit it. */
  readonly canEdit = computed(() => !!this.household() && this.household()!.role !== 'CHILD');

  readonly membersResource = rxResource({
    // Waits for the request context to point at the same family, so the call carries the right header.
    params: () => this.householdId() ?? undefined,
    stream: ({ params }) => this.api.members(params).pipe(catchError(() => of([] as Member[]))),
  });
  readonly members = computed(() => this.membersResource.value() ?? []);

  constructor() {
    const context = inject(RequestContext);
    effect(() => context.householdId.set(this.householdId()));
    effect(() => {
      if (context.unauthorized() > 0) {
        untracked(() => this.sessionExpired());
      }
    });
    this.api.authConfig().pipe(catchError(() => of(null))).subscribe((c) => c && this.config.set(c));
    this.refresh().subscribe();
  }

  refresh(): Observable<Me | null> {
    return this.api.me().pipe(
      catchError((e: HttpErrorResponse) => of(e.status === 401 ? null : this.me() ?? null)),
      tap((me) => this.me.set(me)),
    );
  }

  select(householdId: string): void {
    this.selectedId.set(householdId);
    write(HOUSEHOLD_KEY, householdId);
  }

  startDemo(): Observable<Me | null> {
    return new Observable<Me | null>((subscriber) => {
      this.api.startDemo().subscribe({
        next: () => this.refresh().subscribe(subscriber),
        error: (e) => subscriber.error(e),
      });
    });
  }

  logout(): void {
    this.api.logout().subscribe(() => {
      this.me.set(null);
      write(HOUSEHOLD_KEY, null);
    });
  }

  /** Called when an API call answers 401: the session cookie expired. */
  sessionExpired(): void {
    if (this.me()) {
      this.me.set(null);
    }
  }

  rememberInvite(token: string | null): void {
    write(PENDING_INVITE_KEY, token);
  }

  pendingInvite(): string | null {
    return read(PENDING_INVITE_KEY);
  }
}

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value) {
      localStorage.setItem(key, value);
    } else {
      localStorage.removeItem(key);
    }
  } catch {
    // Storage may be unavailable (private mode); the app still works for this visit.
  }
}
