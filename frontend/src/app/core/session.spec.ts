import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Session } from './session';
import { apiInterceptor } from './api-interceptor';
import { Api } from './api';
import { Me } from './models';

const me: Me = {
  user: { id: 'u1', name: 'Moriya', email: 'm@example.com', picture: null, demo: false },
  households: [
    { id: 'h1', name: 'Eldar', inIsrael: true, demo: false, memberId: 'm1', role: 'ADMIN' },
    { id: 'h2', name: 'Grandma', inIsrael: true, demo: false, memberId: 'm2', role: 'CHILD' },
  ],
};

describe('Session and API interceptor', () => {
  let http: HttpTestingController;
  let session: Session;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([apiInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    session = TestBed.inject(Session);
    http.expectOne('/auth/config').flush({ googleEnabled: true, googleLoginUrl: '/oauth2/authorization/google' });
  });

  afterEach(() => http.verify());

  it('treats a 401 from /api/me as "not logged in"', () => {
    http.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(session.me()).toBeNull();
    expect(session.loggedIn()).toBe(false);
    expect(session.config().googleEnabled).toBe(true);
  });

  it('picks the first family and knows the role in it', () => {
    http.expectOne('/api/me').flush(me);
    TestBed.tick();
    http.expectOne('/api/households/h1/members').flush([]);

    expect(session.householdId()).toBe('h1');
    expect(session.isAdmin()).toBe(true);
    expect(session.canEdit()).toBe(true);

    session.select('h2');
    TestBed.tick();
    http.expectOne('/api/households/h2/members').flush([]);
    expect(session.canEdit()).toBe(false);
    expect(localStorage.getItem('luach.household')).toBe('h2');
  });

  it('sends the CSRF header and the family header with every call', () => {
    http.expectOne('/api/me').flush(me);
    TestBed.tick();
    http.expectOne('/api/households/h1/members').flush([]);

    TestBed.inject(Api).week('2026-10-04').subscribe();
    const request = http.expectOne((r) => r.url === '/api/week');
    expect(request.request.headers.get('X-Requested-With')).toBe('luach');
    expect(request.request.headers.get('X-Household-Id')).toBe('h1');
    request.flush({ start: '2026-10-04', days: [], unscheduled: [] });
  });

  it('notices an expired session', () => {
    http.expectOne('/api/me').flush(me);
    TestBed.tick();
    http.expectOne('/api/households/h1/members').flush([]);

    TestBed.inject(Api).week('2026-10-04').subscribe({ error: () => undefined });
    http.expectOne((r) => r.url === '/api/week').flush(null, { status: 401, statusText: 'Unauthorized' });
    TestBed.tick();

    expect(session.me()).toBeNull();
  });
});
