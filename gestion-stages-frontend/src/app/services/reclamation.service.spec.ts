import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { ReclamationService } from './reclamation.service';

describe('ReclamationService', () => {
  let service: ReclamationService;
  let http: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReclamationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('opens a complaint with its first message', () => {
    service.creerReclamation(30, 'NOTE', 'Note de stage', 'Ma note est fausse').subscribe();

    const req = http.expectOne(`${api}/stages/30/reclamations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      typeReclamation: 'NOTE',
      objet: 'Note de stage',
      messageInitial: 'Ma note est fausse',
    });
    req.flush({});
  });

  it('lists complaints for a stage and for the company', () => {
    service.listerReclamations(30).subscribe();
    expect(http.expectOne(`${api}/stages/30/reclamations`).request.method).toBe('GET');

    service.listerReclamationsEntreprise().subscribe();
    expect(http.expectOne(`${api}/entreprise/reclamations`).request.method).toBe('GET');
  });

  it('reads the thread and posts a message', () => {
    service.getDetail(40).subscribe();
    expect(http.expectOne(`${api}/reclamations/40`).request.method).toBe('GET');

    service.ajouterMessage(40, 'Bonjour').subscribe();
    const message = http.expectOne(`${api}/reclamations/40/messages`);
    expect(message.request.method).toBe('POST');
    expect(message.request.body.contenu).toBe('Bonjour');
    message.flush({});
  });

  it('calls the resolve, close and reopen transitions with PUT', () => {
    service.resoudre(40).subscribe();
    service.cloturer(40).subscribe();
    service.rouvrir(40).subscribe();

    for (const action of ['resoudre', 'cloturer', 'rouvrir']) {
      expect(http.expectOne(`${api}/reclamations/40/${action}`).request.method).toBe('PUT');
    }
  });
});
