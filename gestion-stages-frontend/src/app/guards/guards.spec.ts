import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { User } from '../models/user.model';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('Route guards', () => {
  let connecte = false;
  let utilisateur: User | null = null;

  beforeEach(() => {
    connecte = false;
    utilisateur = null;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => connecte,
            currentUserSnapshot: () => utilisateur,
          },
        },
      ],
    });
  });

  const route = {} as ActivatedRouteSnapshot;
  const etat = { url: '/espace-etudiant/profil' } as RouterStateSnapshot;
  const url = (resultat: unknown) => TestBed.inject(Router).serializeUrl(resultat as UrlTree);

  it('authGuard lets an authenticated user through', () => {
    connecte = true;

    expect(TestBed.runInInjectionContext(() => authGuard(route, etat))).toBeTrue();
  });

  it('authGuard sends a visitor to login and remembers the requested page', () => {
    const resultat = TestBed.runInInjectionContext(() => authGuard(route, etat));

    expect(url(resultat)).toBe('/login?returnUrl=%2Fespace-etudiant%2Fprofil');
  });

  it('roleGuard accepts an allowed role', () => {
    utilisateur = { id: 1, nom: 'Doe', prenom: 'Lina', email: 'l@x.tn', role: 'ETUDIANT' };

    expect(TestBed.runInInjectionContext(() => roleGuard(['ETUDIANT'])(route, etat))).toBeTrue();
  });

  it('roleGuard sends another role to the access-denied page', () => {
    utilisateur = { id: 2, nom: 'RH', prenom: 'Acme', email: 'rh@x.tn', role: 'ENTREPRISE' };

    const resultat = TestBed.runInInjectionContext(() => roleGuard(['ETUDIANT'])(route, etat));

    expect(url(resultat)).toBe('/interdit');
  });

  it('roleGuard sends a visitor without session to login', () => {
    const resultat = TestBed.runInInjectionContext(() => roleGuard(['SUPER_ADMIN'])(route, etat));

    expect(url(resultat)).toBe('/login');
  });
});
