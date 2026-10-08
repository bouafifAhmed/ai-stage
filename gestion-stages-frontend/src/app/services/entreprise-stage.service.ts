import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  Candidature,
  OffreStage,
  OffreStageRequest,
  StagePage,
  StatutCandidature,
} from '../models/stage.model';

@Injectable({ providedIn: 'root' })
export class EntrepriseStageService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/entreprise`;

  listerOffres(page = 0, size = 10): Observable<StagePage<OffreStage>> {
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

  creerOffre(data: OffreStageRequest): Observable<OffreStage> {
    return this.http.post<OffreStage>(`${this.apiUrl}/offres`, data);
  }

  modifierOffre(
    id: number,
    data: OffreStageRequest,
  ): Observable<OffreStage> {
    return this.http.put<OffreStage>(`${this.apiUrl}/offres/${id}`, data);
  }

  supprimerOffre(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/offres/${id}`);
  }

  listerCandidatures(offreId: number): Observable<StagePage<Candidature>> {
    return this.http.get<StagePage<Candidature>>(
      `${this.apiUrl}/offres/${offreId}/candidatures`,
    );
  }

  obtenirCandidature(id: number): Observable<Candidature> {
    return this.http.get<Candidature>(
      `${this.apiUrl}/candidatures/${id}`,
    );
  }

  modifierStatutCandidature(
    id: number,
    statut: Exclude<StatutCandidature, 'EN_ATTENTE'>,
  ): Observable<Candidature> {
    return this.http.patch<Candidature>(
      `${this.apiUrl}/candidatures/${id}/statut`,
      { statut },
    );
  }
}
