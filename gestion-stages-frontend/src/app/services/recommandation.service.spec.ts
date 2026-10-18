import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { AdequationAnalyse, RecommandationAffichage } from '../models/recommandation.model';
import { RecommandationService } from './recommandation.service';

describe('RecommandationService', () => {
  let service: RecommandationService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/recommandations`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RecommandationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('adds the score percentage to each recommendation, keeping the order', () => {
    let result: RecommandationAffichage[] = [];
    service.obtenirRecommandationsAffichage().subscribe((r) => (result = r));

    const req = http.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush([
      { offreId: 3, nomEntreprise: 'TechCorp', score: 0.87 },
      { offreId: 1, nomEntreprise: 'DataLabs', score: 0.24 },
    ]);

    expect(result.map((r) => r.offreId)).toEqual([3, 1]);
    expect(result.map((r) => r.scorePercentage)).toEqual([87, 24]);
  });

  it('propagates HTTP errors so the page can show its error banner', () => {
    let status = 0;
    service.obtenirRecommandations().subscribe({ error: (e) => (status = e.status) });

    http.expectOne(baseUrl).flush('boom', { status: 500, statusText: 'Server Error' });

    expect(status).toBe(500);
  });

  it('reports the engine as unavailable when the health call fails', () => {
    let disponible: boolean | undefined;
    service.healthCheck().subscribe((d) => (disponible = d));

    http.expectOne(`${baseUrl}/health`).flush(null, { status: 0, statusText: 'Unknown Error' });

    expect(disponible).toBeFalse();
  });

  it('returns the adequation analysis with its source, or null on error', () => {
    let analyse: AdequationAnalyse | null = null;
    service.obtenirAdequation(7).subscribe((a) => (analyse = a));
    http.expectOne(`${baseUrl}/adequation/7`).flush({
      offreId: 7, score: 0.6, scorePourcentage: 60,
      competencesAcquises: ['Python'], competencesManquantes: ['Docker'], conseils: ['Postulez.'],
      correspondances: [{ competenceRequise: 'Python', acquise: true, justification: 'Présent.' }],
      synthese: 'Bon profil.', source: 'ia', modele: 'claude-opus-5-5',
    });
    expect(analyse!.source).toBe('ia');
    expect(analyse!.modele).toBe('claude-opus-5-5');

    let enErreur: AdequationAnalyse | null | undefined;
    service.obtenirAdequation(8).subscribe((a) => (enErreur = a));
    http.expectOne(`${baseUrl}/adequation/8`).flush('', { status: 404, statusText: 'Not Found' });
    expect(enErreur).toBeNull();
  });
});
