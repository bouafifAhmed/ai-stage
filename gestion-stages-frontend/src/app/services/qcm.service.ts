import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  QcmExamen,
  QcmStatut,
  SoumettreQcmRequest,
  SoumettreQcmResponse,
} from '../models/qcm.model';

@Injectable({ providedIn: 'root' })
export class QcmService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/etudiant/qcm/admissibilite`;

  obtenirExamen(): Observable<QcmExamen> {
    return this.http.get<QcmExamen>(this.apiUrl);
  }

  obtenirStatut(): Observable<QcmStatut> {
    return this.http.get<QcmStatut>(`${this.apiUrl}/statut`);
  }

  soumettre(data: SoumettreQcmRequest): Observable<SoumettreQcmResponse> {
    return this.http.post<SoumettreQcmResponse>(`${this.apiUrl}/soumettre`, data);
  }
}
