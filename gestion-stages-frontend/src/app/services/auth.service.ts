import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { CreateEntrepriseRequest } from '../models/entreprise.model';
import { AuthResponse, Role, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  private readonly tokenKey = 'gestion_stages_token';
  private readonly userKey = 'gestion_stages_user';
  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);

  constructor() {
    if (this.isBrowser) {
      this.restoreBrowserSession();
    }
  }

  login(email: string, motDePasse: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, {
        email,
        motDePasse,
      })
      .pipe(tap((response) => this.storeSession(response)));
  }

  register(
    nom: string,
    prenom: string,
    email: string,
    motDePasse: string,
    role: Role,
  ): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, {
        nom,
        prenom,
        email,
        motDePasse,
        role,
      })
      .pipe(
        tap((response) => {
          if (response.token) {
            this.storeSession(response);
          }
        }),
      );
  }

  registerEntreprise(data: CreateEntrepriseRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${environment.apiUrl}/auth/register/entreprise`,
      data,
    );
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.tokenKey);
      localStorage.removeItem(this.userKey);
    }
    this.currentUserSubject.next(null);
    void this.router.navigate(['/login']);
  }

  getCurrentUser(): Observable<User | null> {
    return this.currentUserSubject.asObservable();
  }

  currentUserSnapshot(): User | null {
    return this.currentUserSubject.value;
  }

  getToken(): string | null {
    return this.isBrowser ? localStorage.getItem(this.tokenKey) : null;
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null && this.currentUserSubject.value !== null;
  }

  hasRole(role: Role): boolean {
    return this.currentUserSubject.value?.role === role;
  }

  private storeSession(response: AuthResponse): void {
    if (!response.token) {
      return;
    }
    if (this.isBrowser) {
      localStorage.setItem(this.tokenKey, response.token);
      localStorage.setItem(this.userKey, JSON.stringify(response.user));
    }
    this.currentUserSubject.next(response.user);
  }

  private restoreBrowserSession(): void {
    const user = this.readStoredUser();
    if (user) {
      this.currentUserSubject.next(user);
    }
  }

  private readStoredUser(): User | null {
    if (!this.isBrowser) {
      return null;
    }

    const rawUser = localStorage.getItem(this.userKey);
    const token = localStorage.getItem(this.tokenKey);
    if (!rawUser || !token) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as User;
    } catch {
      localStorage.removeItem(this.userKey);
      localStorage.removeItem(this.tokenKey);
      return null;
    }
  }
}
