import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Role } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let login: jasmine.Spy;
  let navigate: jasmine.Spy;
  let returnUrl: string | null;

  function creer(): LoginComponent {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } },
        },
      ],
    });
    navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function connecterEn(role: Role): void {
    login.and.returnValue(of({ token: 't', type: 'Bearer', user: { id: 1, nom: 'N', prenom: 'P', email: 'e@x.tn', role } }));
  }

  beforeEach(() => {
    login = jasmine.createSpy('login');
    returnUrl = null;
  });

  it('does not call the API while the form is invalid', () => {
    const component = creer();
    component.form.setValue({ email: 'pas-un-email', motDePasse: '' });

    component.submit();

    expect(login).not.toHaveBeenCalled();
    expect(component.form.touched).toBeTrue();
  });

  for (const [role, page] of [
    ['ETUDIANT', '/espace-etudiant'],
    ['ENTREPRISE', '/espace-entreprise'],
    ['CHEF_DEPT_STAGE', '/espace-administration'],
    ['CHEF_DEPT_PEDAGOGIQUE', '/espace-administration'],
    ['SUPER_ADMIN', '/espace-super-admin'],
  ] as [Role, string][]) {
    it(`sends ${role} to ${page}`, () => {
      connecterEn(role);
      const component = creer();
      component.form.setValue({ email: 'e@x.tn', motDePasse: 'secret' });

      component.submit();

      expect(login).toHaveBeenCalledWith('e@x.tn', 'secret');
      expect(navigate).toHaveBeenCalledWith(page);
    });
  }

  it('goes back to the requested internal page after login', () => {
    returnUrl = '/espace-etudiant/profil';
    connecterEn('ETUDIANT');
    const component = creer();
    component.form.setValue({ email: 'e@x.tn', motDePasse: 'secret' });

    component.submit();

    expect(navigate).toHaveBeenCalledWith('/espace-etudiant/profil');
  });

  for (const externe of ['//evil.example', 'https://evil.example']) {
    it(`ignores an external return URL (${externe})`, () => {
      returnUrl = externe;
      connecterEn('ETUDIANT');
      const component = creer();
      component.form.setValue({ email: 'e@x.tn', motDePasse: 'secret' });

      component.submit();

      expect(navigate).toHaveBeenCalledWith('/espace-etudiant');
    });
  }

  it('shows the server message on failure and stops loading', () => {
    login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403, error: { message: 'Compte en attente' } })));
    const component = creer();
    component.form.setValue({ email: 'e@x.tn', motDePasse: 'secret' });

    component.submit();

    expect(component.serverError).toBe('Compte en attente');
    expect(component.loading).toBeFalse();
    expect(navigate).not.toHaveBeenCalled();
  });
});
