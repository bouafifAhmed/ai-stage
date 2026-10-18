import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../services/auth.service';
import { CalendrierEcheancesComponent } from './calendrier-echeances.component';

describe('CalendrierEcheancesComponent', () => {
  let http: HttpTestingController;
  let connecte: boolean;

  function creer(params: Record<string, string>, role: 'ETUDIANT' | 'ENTREPRISE'): ComponentFixture<CalendrierEcheancesComponent> {
    TestBed.configureTestingModule({
      imports: [CalendrierEcheancesComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => connecte } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(params), data: { role } } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CalendrierEcheancesComponent);
    fixture.componentInstance.currentYear = 2026;
    fixture.componentInstance.currentMonth = 10;
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => (connecte = true));
  afterEach(() => http.verify());

  it('builds a Monday-first grid of whole weeks for October 2026', () => {
    const component = creer({}, 'ETUDIANT').componentInstance;
    http.expectOne((r) => r.url === `${environment.apiUrl}/etudiant/echeances`).flush([
      { tacheId: 1, stageId: 30, titre: 'Rapport', dateEcheance: '2026-10-20', statut: 'ASSIGNEE',
        nomEtudiant: 'Lina Doe', entrepriseNom: 'Acme', offreTitre: 'Stage Java' },
    ]);

    const cells = component.cells;

    expect(cells.length % 7).toBe(0);
    // Le 1er octobre 2026 est un jeudi : trois jours de septembre avant lui.
    expect(cells.slice(0, 3).map((c) => [c.day, c.inMonth])).toEqual([[28, false], [29, false], [30, false]]);
    expect(cells[3].day).toBe(1);
    expect(cells.filter((c) => c.inMonth).length).toBe(31);
    expect(cells.find((c) => c.inMonth && c.day === 20)!.items.map((i) => i.titre)).toEqual(['Rapport']);
    expect(component.monthLabel).toContain('2026');
  });

  it('crosses the year boundary when changing month and reloads', () => {
    const component = creer({}, 'ETUDIANT').componentInstance;
    http.expectOne((r) => r.url.endsWith('/etudiant/echeances')).flush([]);

    component.currentMonth = 1;
    component.previousMonth();
    const precedent = http.expectOne((r) => r.url.endsWith('/etudiant/echeances'));
    expect(precedent.request.params.get('annee')).toBe('2025');
    expect(precedent.request.params.get('mois')).toBe('12');
    precedent.flush([]);

    component.nextMonth();
    const suivant = http.expectOne((r) => r.url.endsWith('/etudiant/echeances'));
    expect(suivant.request.params.get('annee')).toBe('2026');
    expect(suivant.request.params.get('mois')).toBe('1');
    suivant.flush([]);
  });

  it('uses the stage calendar and company links when a stage is given', () => {
    const component = creer({ stageId: '30' }, 'ENTREPRISE').componentInstance;

    http.expectOne((r) => r.url === `${environment.apiUrl}/stages/30/echeances`).flush([]);
    expect(component.tachesLink).toEqual(['/espace-entreprise/stages', 30, 'taches-assignees', 'validation']);
    expect(component.homeLink).toBe('/espace-entreprise');
  });

  it('asks to log in instead of calling the API without a session', () => {
    connecte = false;
    const component = creer({}, 'ETUDIANT').componentInstance;

    expect(component.errorMessage).toContain('connecter');
    expect(component.loading).toBeFalse();
  });

  it('shows the API error message', () => {
    const component = creer({}, 'ETUDIANT').componentInstance;

    http.expectOne((r) => r.url.endsWith('/etudiant/echeances'))
      .flush({ message: 'Accès refusé' }, { status: 403, statusText: 'Forbidden' });

    expect(component.errorMessage).toBe('Accès refusé');
  });
});
