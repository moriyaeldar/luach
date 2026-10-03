import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./schedule/schedule-page').then((m) => m.SchedulePage) },
  { path: 'join/:token', loadComponent: () => import('./join/join-page').then((m) => m.JoinPage) },
  { path: '**', redirectTo: '' },
];
