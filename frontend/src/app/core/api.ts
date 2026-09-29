import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Member, PreviewItem, Recurrence, Task, TaskRequest, WeekResponse } from './models';

/** Talks to the API Gateway. All paths are relative, so the same build works behind any host. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  week(start: string): Observable<WeekResponse> {
    return this.http.get<WeekResponse>('/api/week', { params: { start } });
  }

  members(): Observable<Member[]> {
    return this.http.get<Member[]>('/api/members');
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
