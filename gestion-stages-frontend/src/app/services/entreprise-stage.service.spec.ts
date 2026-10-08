import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { OffreStageRequest } from '../models/stage.model';
import { EntrepriseStageService } from './entreprise-stage.service';

describe('EntrepriseStageService', () => {
  let service: EntrepriseStageService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/entreprise`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EntrepriseStageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a paginated offers list', () => {
    service.listerOffres(2, 5).subscribe();
    const request = http.expectOne(
      (item) =>
        item.url === `${baseUrl}/offres` &&
        item.params.get('page') === '2' &&
        item.params.get('size') === '5',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('creates and updates an offer', () => {
    const payload = {
      titre: 'Stage Angular',
      description: 'Une description suffisamment détaillée.',
    } as OffreStageRequest;

    service.creerOffre(payload).subscribe();
    const create = http.expectOne(`${baseUrl}/offres`);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(payload);
    create.flush({ id: 1 });

    service.modifierOffre(1, payload).subscribe();
    const update = http.expectOne(`${baseUrl}/offres/1`);
    expect(update.request.method).toBe('PUT');
    update.flush({ id: 1 });
  });

  it('loads applications and candidate details', () => {
    service.listerCandidatures(4).subscribe();
    const list = http.expectOne(`${baseUrl}/offres/4/candidatures`);
    expect(list.request.method).toBe('GET');
    list.flush({ content: [], totalElements: 0, totalPages: 0 });

    service.obtenirCandidature(9).subscribe();
    const detail = http.expectOne(`${baseUrl}/candidatures/9`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 9 });

    service.modifierStatutCandidature(9, 'ACCEPTEE').subscribe();
    const decision = http.expectOne(`${baseUrl}/candidatures/9/statut`);
    expect(decision.request.method).toBe('PATCH');
    expect(decision.request.body).toEqual({ statut: 'ACCEPTEE' });
    decision.flush({ id: 9, statut: 'ACCEPTEE' });
  });
});
