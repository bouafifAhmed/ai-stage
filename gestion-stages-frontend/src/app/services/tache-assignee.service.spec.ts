import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { TacheAssigneeService } from './tache-assignee.service';

describe('TacheAssigneeService', () => {
  let service: TacheAssigneeService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/stages/12/taches-assignees`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TacheAssigneeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists and filters assigned tasks', () => {
    service.lister(12, 'TERMINEE').subscribe();
    const request = http.expectOne(
      (item) => item.url === baseUrl && item.params.get('statut') === 'TERMINEE',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('assigns a task in the candidature stage', () => {
    const payload = {
      titre: 'Compte rendu',
      description: 'Préparer le compte rendu hebdomadaire.',
      dateEcheance: '2026-09-08',
    };
    service.assigner(12, payload).subscribe();
    const request = http.expectOne(baseUrl);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 4 });
  });

  it('completes a task using the expected multipart fields', () => {
    const file = new File(['résultat'], 'resultat.txt', { type: 'text/plain' });
    service.terminer(12, 4, 'Travail terminé', file).subscribe();
    const request = http.expectOne(`${baseUrl}/4/terminer`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body instanceof FormData).toBeTrue();
    expect((request.request.body as FormData).get('commentaireEtudiant')).toBe('Travail terminé');
    expect((request.request.body as FormData).get('pieceJointe')).toBe(file);
    request.flush({ id: 4, statut: 'TERMINEE' });
  });

  it('validates and rejects with the stage id in each URL', () => {
    service.valider(12, 4, 'Conforme').subscribe();
    const validation = http.expectOne(`${baseUrl}/4/valider`);
    expect(validation.request.method).toBe('PUT');
    expect(validation.request.body).toEqual({ commentaireEncadrant: 'Conforme' });
    validation.flush({ id: 4, statut: 'VALIDEE' });

    service.rejeter(12, 5, 'À corriger').subscribe();
    const rejection = http.expectOne(`${baseUrl}/5/rejeter`);
    expect(rejection.request.method).toBe('PUT');
    expect(rejection.request.body).toEqual({ commentaireEncadrant: 'À corriger' });
    rejection.flush({ id: 5, statut: 'REJETEE' });
  });

  it('downloads an attachment as a blob', () => {
    service.telechargerPieceJointe(12, 4).subscribe();
    const request = http.expectOne(`${baseUrl}/4/piece-jointe`);
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['contenu']));
  });
});
