import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let jeton: string | null;
  let logout: jasmine.Spy;

  beforeEach(() => {
    jeton = 'jwt-token';
    logout = jasmine.createSpy('logout');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => jeton, logout } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('adds the bearer token to API calls', () => {
    http.get('/api/etudiant/profil').subscribe();

    const req = backend.expectOne('/api/etudiant/profil');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush({});
  });

  it('never sends the token to the login and register endpoints', () => {
    http.post('/api/auth/login', {}).subscribe();
    http.post('/api/auth/register', {}).subscribe();

    expect(backend.expectOne('/api/auth/login').request.headers.has('Authorization')).toBeFalse();
    expect(backend.expectOne('/api/auth/register').request.headers.has('Authorization')).toBeFalse();
  });

  it('sends no header when the user is not logged in', () => {
    jeton = null;
    http.get('/api/etudiant/profil').subscribe();

    expect(backend.expectOne('/api/etudiant/profil').request.headers.has('Authorization')).toBeFalse();
  });

  it('logs out on 401 but not on 403', () => {
    http.get('/api/etudiant/profil').subscribe({ error: () => undefined });
    backend.expectOne('/api/etudiant/profil').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(logout).not.toHaveBeenCalled();

    http.get('/api/etudiant/profil').subscribe({ error: () => undefined });
    backend.expectOne('/api/etudiant/profil').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('does not log out when a failed login returns 401', () => {
    let statut = 0;
    http.post('/api/auth/login', {}).subscribe({ error: (e) => (statut = e.status) });

    backend.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(statut).toBe(401);
    expect(logout).not.toHaveBeenCalled();
  });
});
