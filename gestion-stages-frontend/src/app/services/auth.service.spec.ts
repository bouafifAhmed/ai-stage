import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../environments/environment';
import { AuthResponse } from '../models/user.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('stores the token and current user after login', () => {
    const response: AuthResponse = {
      token: 'jwt-token',
      type: 'Bearer',
      user: {
        id: 1,
        nom: 'Dupont',
        prenom: 'Lina',
        email: 'lina@example.com',
        role: 'ETUDIANT',
      },
    };

    service.login('lina@example.com', 'motdepasse').subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    request.flush(response);

    expect(service.getToken()).toBe('jwt-token');
    expect(service.currentUserSnapshot()).toEqual(response.user);
    expect(service.isAuthenticated()).toBeTrue();
  });
});
