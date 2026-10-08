import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  CreateEntrepriseRequest,
  Entreprise,
  PageResponse,
  StatutValidation,
} from '../models/entreprise.model';

@Injectable({ providedIn: 'root' })
export class EntrepriseService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/entreprises`;

  creerEntreprise(data: CreateEntrepriseRequest): Observable<Entreprise> {
    return this.http.post<Entreprise>(this.apiUrl, data);
  }

  modifierEntreprise(
    id: number,
    data: Partial<CreateEntrepriseRequest>,
  ): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}`, data);
  }

  desactiverEntreprise(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  validerEntreprise(id: number): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}/validation`, {});
  }

  listerEntreprises(
    statut?: StatutValidation,
    page = 0,
    size = 10,
  ): Observable<PageResponse<Entreprise>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'nom,asc');

    if (statut) {
      params = params.set('statut', statut);
    }

    return this.http.get<PageResponse<Entreprise>>(this.apiUrl, { params });
  }
}
