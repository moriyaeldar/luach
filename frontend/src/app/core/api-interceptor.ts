import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { RequestContext } from './request-context';

/**
 * Adds the headers the Gateway expects:
 * - X-Requested-With on every call (the Gateway's CSRF check for state-changing requests)
 * - X-Household-Id: which family the call is about
 * and reports an expired session (401).
 */
export const apiInterceptor: HttpInterceptorFn = (request, next) => {
  const context = inject(RequestContext);
  const household = context.householdId();
  const headers: Record<string, string> = { 'X-Requested-With': 'luach' };
  if (household && !request.headers.has('X-Household-Id')) {
    headers['X-Household-Id'] = household;
  }
  return next(request.clone({ setHeaders: headers })).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.endsWith('/api/me')) {
        context.unauthorized.update((n) => n + 1);
      }
      return throwError(() => error);
    }),
  );
};
