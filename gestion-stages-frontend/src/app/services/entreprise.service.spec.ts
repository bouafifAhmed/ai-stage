import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { EntrepriseService } from './entreprise.service';

describe('EntrepriseService', () => {
  let service: EntrepriseService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EntrepriseService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('requests a filtered page of entreprises', () => {
    service.listerEntreprises('VALIDEE', 2).subscribe();

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === `${environment.apiUrl}/admin/entreprises` &&
        candidate.params.get('statut') === 'VALIDEE' &&
        candidate.params.get('page') === '2',
    );
    expect(request.request.method).toBe('GET');
    request.flush({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 2,
      first: false,
      last: true,
    });
  });
});
