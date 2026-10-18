import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { SoumettreQcmRequest } from '../models/qcm.model';
import { QcmService } from './qcm.service';

describe('QcmService', () => {
  let service: QcmService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/etudiant/qcm/admissibilite`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(QcmService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the exam and the attempt status', () => {
    service.obtenirExamen().subscribe();
    expect(http.expectOne(baseUrl).request.method).toBe('GET');

    service.obtenirStatut().subscribe();
    expect(http.expectOne(`${baseUrl}/statut`).request.method).toBe('GET');
  });

  it('submits the answers in the request body', () => {
    const reponses = { reponses: [{ questionId: 1, optionIds: [2] }] } as unknown as SoumettreQcmRequest;

    service.soumettre(reponses).subscribe();

    const req = http.expectOne(`${baseUrl}/soumettre`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(reponses);
    req.flush({});
  });
});
