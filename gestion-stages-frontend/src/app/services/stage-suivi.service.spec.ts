import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { StageSuiviService } from './stage-suivi.service';

describe('StageSuiviService', () => {
  let service: StageSuiviService;
  let http: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StageSuiviService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks the deadlines of a month for a stage and for the student', () => {
    service.echeancesStage(30, 2026, 10).subscribe();
    const stage = http.expectOne((r) => r.url === `${api}/stages/30/echeances`);
    expect(stage.request.params.get('annee')).toBe('2026');
    expect(stage.request.params.get('mois')).toBe('10');
    stage.flush([]);

    service.echeancesEtudiant(2026, 2).subscribe();
    const etudiant = http.expectOne((r) => r.url === `${api}/etudiant/echeances`);
    expect(etudiant.request.params.get('mois')).toBe('2');
    etudiant.flush([]);
  });

  it('reads the closing state and sends both signatures', () => {
    service.etatCloture(30).subscribe();
    expect(http.expectOne(`${api}/stages/30/cloture`).request.method).toBe('GET');

    service.signerEtudiant(30, { signatureBase64: 'abc' }).subscribe();
    const etudiant = http.expectOne(`${api}/stages/30/signature/etudiant`);
    expect(etudiant.request.body).toEqual({ signatureBase64: 'abc' });
    etudiant.flush({});

    service.signerEncadrant(30, { signatureBase64: 'def' }).subscribe();
    expect(http.expectOne(`${api}/stages/30/signature/encadrant`).request.method).toBe('POST');
  });

  it('downloads the report as a blob', () => {
    let taille = 0;
    service.telechargerRapport(30).subscribe((blob) => (taille = blob.size));

    const req = http.expectOne(`${api}/stages/30/rapport-pdf`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF']));
    expect(taille).toBe(4);
  });
});
