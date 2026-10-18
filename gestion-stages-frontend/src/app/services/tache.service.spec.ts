import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { TacheService } from './tache.service';

describe('TacheService', () => {
  let service: TacheService;
  let http: HttpTestingController;
  const journal = `${environment.apiUrl}/stages/30/journal`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TacheService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates, updates and lists journal entries', () => {
    service.creerTache(30, { titre: 'Analyse' }).subscribe();
    const creation = http.expectOne(journal);
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ titre: 'Analyse' });
    creation.flush({});

    service.modifierTache(30, 7, { titre: 'Analyse v2' }).subscribe();
    expect(http.expectOne(`${journal}/7`).request.method).toBe('PUT');

    service.listerTaches(30).subscribe();
    expect(http.expectOne(journal).request.method).toBe('GET');

    service.getProgression(30).subscribe();
    expect(http.expectOne(`${journal}/progression`).request.method).toBe('GET');
  });

  it('uploads the attachment as multipart form data', () => {
    const fichier = new File(['%PDF'], 'preuve.pdf', { type: 'application/pdf' });

    service.uploaderFichier(30, fichier).subscribe();

    const req = http.expectOne(`${journal}/upload`);
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('file')).toEqual(fichier);
    req.flush({ chemin: '/uploads/journal/30/x.pdf' });
  });

  it('downloads the attachment and the journal PDF as blobs', () => {
    service.telechargerPieceJointe(30, 7).subscribe();
    expect(http.expectOne(`${journal}/7/piece-jointe`).request.responseType).toBe('blob');

    service.telechargerJournalPdf(30).subscribe();
    expect(http.expectOne(`${journal}/pdf`).request.responseType).toBe('blob');
  });
});
