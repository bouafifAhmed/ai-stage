import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  EcheanceCalendrier,
  SignerStageRequest,
  StageCloture,
} from '../models/stage-suivi.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StageSuiviService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/stages`;

  echeancesStage(stageId: number, annee: number, mois: number): Observable<EcheanceCalendrier[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<EcheanceCalendrier[]>(`${this.apiUrl}/${stageId}/echeances`, { params });
  }

  echeancesEtudiant(annee: number, mois: number): Observable<EcheanceCalendrier[]> {
    const params = new HttpParams().set('annee', annee).set('mois', mois);
    return this.http.get<EcheanceCalendrier[]>(`${environment.apiUrl}/etudiant/echeances`, { params });
  }

  etatCloture(stageId: number): Observable<StageCloture> {
    return this.http.get<StageCloture>(`${this.apiUrl}/${stageId}/cloture`);
  }

  signerEtudiant(stageId: number, request: SignerStageRequest): Observable<StageCloture> {
    return this.http.post<StageCloture>(`${this.apiUrl}/${stageId}/signature/etudiant`, request);
  }

  signerEncadrant(stageId: number, request: SignerStageRequest): Observable<StageCloture> {
    return this.http.post<StageCloture>(`${this.apiUrl}/${stageId}/signature/encadrant`, request);
  }

  telechargerRapport(stageId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${stageId}/rapport-pdf`, { responseType: 'blob' });
  }
}
