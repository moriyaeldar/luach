import { Injectable, signal } from '@angular/core';

/**
 * The little state the HTTP interceptor needs, kept apart from {@link Session} so the interceptor doesn't depend on a
 * service that itself makes HTTP calls (which would be a circular dependency).
 */
@Injectable({ providedIn: 'root' })
export class RequestContext {
  /** The family the app is showing; sent as X-Household-Id. */
  readonly householdId = signal<string | null>(null);
  /** Bumped when an API call answers 401 (the session cookie expired). */
  readonly unauthorized = signal(0);
}
