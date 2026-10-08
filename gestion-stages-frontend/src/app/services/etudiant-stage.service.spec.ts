import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { EtudiantStageService } from './etudiant-stage.service';

describe('EtudiantStageService', () => {
  let service: EtudiantStageService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/etudiant`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EtudiantStageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads offers and an offer detail', () => {
    service.listerOffres(1).subscribe();
    const list = http.expectOne(
      (item) => item.url === `${baseUrl}/offres` && item.params.get('page') === '1',
    );
    expect(list.request.method).toBe('GET');
    list.flush({ content: [], totalElements: 0, totalPages: 0 });

    service.obtenirOffre(3).subscribe();
    const detail = http.expectOne(`${baseUrl}/offres/3`);
    expect(detail.request.method).toBe('GET');
    detail.flush({ id: 3 });
  });

  it('gets and updates the student profile', () => {
    service.obtenirProfil().subscribe();
    const get = http.expectOne(`${baseUrl}/profil`);
    expect(get.request.method).toBe('GET');
    get.flush({});

    const payload = {
      telephone: '20000000',
      filiere: 'Informatique',
      niveauEtudes: 'Master',
      competences: ['Angular'],
    };
    service.modifierProfil(payload).subscribe();
    const put = http.expectOne(`${baseUrl}/profil`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual(payload);
    put.flush(payload);
  });

  it('uploads, extracts, downloads and deletes the student CV', () => {
    const file = new File(['%PDF-1.7'], 'cv.pdf', { type: 'application/pdf' });

    service.uploaderCv(file).subscribe();
    const upload = http.expectOne(`${baseUrl}/profil/cv`);
    expect(upload.request.method).toBe('POST');
    expect(upload.request.body instanceof FormData).toBeTrue();
    expect((upload.request.body as FormData).get('file')).toBe(file);
    upload.flush({
      profil: { cvPresent: true, cvNomFichier: 'cv.pdf' },
      extraction: { texteDetecte: true, competences: ['Angular'] },
    });

    service.extraireCv().subscribe();
    const extraction = http.expectOne(`${baseUrl}/profil/cv/extraction`);
    expect(extraction.request.method).toBe('POST');
    extraction.flush({ texteDetecte: true, competences: ['Angular'] });

    service.telechargerCv().subscribe();
    const download = http.expectOne(`${baseUrl}/profil/cv`);
    expect(download.request.method).toBe('GET');
    expect(download.request.responseType).toBe('blob');
    download.flush(new Blob(['%PDF-1.7'], { type: 'application/pdf' }));

    service.supprimerCv().subscribe();
    const remove = http.expectOne(`${baseUrl}/profil/cv`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });

  it('submits and lists applications', () => {
    service.postuler({ offreId: 7, message: 'Ma motivation détaillée.' }).subscribe();
    const post = http.expectOne(`${baseUrl}/candidatures`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.offreId).toBe(7);
    post.flush({ id: 1 });

    service.listerCandidatures().subscribe();
    const list = http.expectOne(`${baseUrl}/candidatures`);
    expect(list.request.method).toBe('GET');
    list.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('loads and marks student notifications as read', () => {
    service.compterNotificationsNonLues().subscribe();
    const count = http.expectOne(`${baseUrl}/notifications/non-lues`);
    expect(count.request.method).toBe('GET');
    count.flush({ nonLues: 1 });

    service.listerNotifications().subscribe();
    const list = http.expectOne(`${baseUrl}/notifications`);
    expect(list.request.method).toBe('GET');
    list.flush({ content: [], totalElements: 0, totalPages: 0 });

    service.marquerNotificationLue(5).subscribe();
    const mark = http.expectOne(`${baseUrl}/notifications/5/lue`);
    expect(mark.request.method).toBe('PATCH');
    mark.flush({ id: 5, lue: true });
  });
});
