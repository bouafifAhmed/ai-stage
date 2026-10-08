import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  CvExtraction,
  CvUploadResult,
  ProfilEtudiant,
  ProfilEtudiantRequest,
} from '../models/profil-etudiant.model';
import {
  Candidature,
  CreateCandidatureRequest,
  NotificationEtudiant,
  OffreStage,
  StagePage,
} from '../models/stage.model';

@Injectable({ providedIn: 'root' })
export class EtudiantStageService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/etudiant`;

  listerOffres(page = 0, size = 9): Observable<StagePage<OffreStage>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'dateCreation,desc');
    return this.http.get<StagePage<OffreStage>>(`${this.apiUrl}/offres`, {
      params,
    });
  }

  obtenirOffre(id: number): Observable<OffreStage> {
    return this.http.get<OffreStage>(`${this.apiUrl}/offres/${id}`);
  }

  obtenirProfil(): Observable<ProfilEtudiant> {
    return this.http.get<ProfilEtudiant>(`${this.apiUrl}/profil`);
  }

  modifierProfil(data: ProfilEtudiantRequest): Observable<ProfilEtudiant> {
    return this.http.put<ProfilEtudiant>(`${this.apiUrl}/profil`, data);
  }

  uploaderCv(file: File): Observable<CvUploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CvUploadResult>(`${this.apiUrl}/profil/cv`, formData);
  }

  extraireCv(): Observable<CvExtraction> {
    return this.http.post<CvExtraction>(`${this.apiUrl}/profil/cv/extraction`, null);
  }

  telechargerCv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/profil/cv`, { responseType: 'blob' });
  }

  supprimerCv(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/profil/cv`);
  }

  postuler(data: CreateCandidatureRequest): Observable<Candidature> {
    return this.http.post<Candidature>(
      `${this.apiUrl}/candidatures`,
      data,
    );
  }

  listerCandidatures(): Observable<StagePage<Candidature>> {
    return this.http.get<StagePage<Candidature>>(`${this.apiUrl}/candidatures`);
  }

  listerNotifications(): Observable<StagePage<NotificationEtudiant>> {
    return this.http.get<StagePage<NotificationEtudiant>>(`${this.apiUrl}/notifications`);
  }

  compterNotificationsNonLues(): Observable<{ nonLues: number }> {
    return this.http.get<{ nonLues: number }>(`${this.apiUrl}/notifications/non-lues`);
  }

  marquerNotificationLue(id: number): Observable<NotificationEtudiant> {
    return this.http.patch<NotificationEtudiant>(
      `${this.apiUrl}/notifications/${id}/lue`,
      null,
    );
  }
}
