import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AuthConfig, InvitePreview, Me, Member, Membership, PreviewItem, Recurrence, Role, Task, TaskRequest, WeekResponse,
} from './models';

/**
 * Talks to the Gateway. All paths are relative (same origin as the app), the session travels in an HttpOnly cookie,
 * and the family header is added by {@link apiInterceptor}.
 */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  // --- login -------------------------------------------------------------------------------------------------
  authConfig(): Observable<AuthConfig> {
    return this.http.get<AuthConfig>('/auth/config');
  }

  startDemo(): Observable<void> {
    return this.http.post<void>('/auth/demo', null);
  }

  logout(): Observable<void> {
    return this.http.post<void>('/auth/logout', null);
  }

  // --- families ----------------------------------------------------------------------------------------------
  me(): Observable<Me> {
    return this.http.get<Me>('/api/me');
  }

  createHousehold(name: string, inIsrael: boolean): Observable<Membership> {
    return this.http.post<Membership>('/api/households', { name, inIsrael });
  }

  updateHousehold(id: string, name: string, inIsrael: boolean): Observable<Membership> {
    return this.http.patch<Membership>(`/api/households/${id}`, { name, inIsrael });
  }

  members(householdId: string): Observable<Member[]> {
    return this.http.get<Member[]>(`/api/households/${householdId}/members`);
  }

  addMember(householdId: string, displayName: string, role: Role, color: string): Observable<Member> {
    return this.http.post<Member>(`/api/households/${householdId}/members`, { displayName, role, color });
  }

  updateMember(householdId: string, memberId: string, change: Partial<Pick<Member, 'displayName' | 'role' | 'color'>>):
    Observable<Member> {
    return this.http.patch<Member>(`/api/households/${householdId}/members/${memberId}`, change);
  }

  removeMember(householdId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`/api/households/${householdId}/members/${memberId}`);
  }

  createInvite(householdId: string, invite: { memberId?: string; displayName?: string; role?: Role }):
    Observable<{ token: string; expiresAt: string }> {
    return this.http.post<{ token: string; expiresAt: string }>(`/api/households/${householdId}/invites`, invite);
  }

  invite(token: string): Observable<InvitePreview> {
    return this.http.get<InvitePreview>(`/api/invites/${token}`);
  }

  acceptInvite(token: string): Observable<Membership> {
    return this.http.post<Membership>(`/api/invites/${token}/accept`, null);
  }

  // --- tasks -------------------------------------------------------------------------------------------------
  week(start: string): Observable<WeekResponse> {
    return this.http.get<WeekResponse>('/api/week', { params: { start } });
  }

  task(id: string): Observable<Task> {
    return this.http.get<Task>(`/api/tasks/${id}`);
  }

  create(task: TaskRequest): Observable<Task> {
    return this.http.post<Task>('/api/tasks', task);
  }

  update(id: string, task: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${id}`, task);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${id}`);
  }

  setDone(id: string, date: string, done: boolean): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${id}/completion`, { date, done });
  }

  preview(recurrence: Recurrence, from: string, count = 5): Observable<{ occurrences: PreviewItem[] }> {
    return this.http.post<{ occurrences: PreviewItem[] }>('/api/recurrence/preview', { recurrence, from, count });
  }
}
