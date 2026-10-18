import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { CreateEtudiantRequest, UpdateEtudiantRequest } from '../models/etudiant-admin.model';
import { AdminEtudiantService } from './admin-etudiant.service';

describe('AdminEtudiantService', () => {
  let service: AdminEtudiantService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/admin/etudiants`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminEtudiantService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists students sorted by name with default paging', () => {
    service.listerEtudiants().subscribe();

    const req = http.expectOne((r) => r.url === baseUrl);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('sort')).toBe('nom,asc');
    expect(req.request.params.has('actif')).toBeFalse();
    expect(req.request.params.has('filiere')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('adds the active and field-of-study filters when given', () => {
    service.listerEtudiants({ actif: false, filiere: 'Info', page: 2, size: 5 }).subscribe();

    const req = http.expectOne((r) => r.url === baseUrl);
    expect(req.request.params.get('actif')).toBe('false');
    expect(req.request.params.get('filiere')).toBe('Info');
    expect(req.request.params.get('page')).toBe('2');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('creates, updates, deactivates and reactivates a student', () => {
    service.creerEtudiant({ nom: 'Doe' } as CreateEtudiantRequest).subscribe();
    expect(http.expectOne(baseUrl).request.method).toBe('POST');

    service.modifierEtudiant(4, { filiere: 'Info' } as UpdateEtudiantRequest).subscribe();
    expect(http.expectOne(`${baseUrl}/4`).request.method).toBe('PUT');

    service.desactiverEtudiant(4).subscribe();
    expect(http.expectOne(`${baseUrl}/4`).request.method).toBe('DELETE');

    service.activerEtudiant(4).subscribe();
    const activation = http.expectOne(`${baseUrl}/4/activation`);
    expect(activation.request.method).toBe('PUT');
    activation.flush({});
  });
});
