import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { RecommandationsComponent } from './recommandations.component';

describe('RecommandationsComponent', () => {
  let fixture: ComponentFixture<RecommandationsComponent>;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/recommandations`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RecommandationsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(RecommandationsComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const texte = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('shows the sorted list and filters strong matches', () => {
    http.expectOne(baseUrl).flush([
      { offreId: 3, nomEntreprise: 'TechCorp', score: 0.8 },
      { offreId: 1, nomEntreprise: 'DataLabs', score: 0.3 },
    ]);
    fixture.detectChanges();

    const noms = () => Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.card-entreprise'),
    ).map((e) => e.textContent?.trim());
    expect(noms()).toEqual(['TechCorp', 'DataLabs']);

    fixture.componentInstance.filtreScore = 'HAUT';
    fixture.detectChanges();
    expect(noms()).toEqual(['TechCorp']);
  });

  it('shows the error banner when the API fails', () => {
    http.expectOne(baseUrl).flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.hasError).toBeTrue();
    expect(texte()).toContain('Impossible de charger les recommandations');
  });

  it('reports a stopped engine instead of an empty list', () => {
    http.expectOne(baseUrl).flush([]);
    http.expectOne(`${baseUrl}/health`).flush(false);
    fixture.detectChanges();

    expect(texte()).toContain('moteur de recommandation est momentanément indisponible');
    expect(texte()).not.toContain('Aucune recommandation disponible');
  });

  it('keeps the empty message when the engine is up', () => {
    http.expectOne(baseUrl).flush([]);
    http.expectOne(`${baseUrl}/health`).flush(true);
    fixture.detectChanges();

    expect(texte()).toContain('Aucune recommandation disponible');
  });
});
