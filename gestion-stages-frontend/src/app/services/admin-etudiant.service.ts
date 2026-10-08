import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  CreateEtudiantRequest,
  EtudiantAdmin,
  EtudiantPageResponse,
  UpdateEtudiantRequest,
} from '../models/etudiant-admin.model';

@Injectable({ providedIn: 'root' })
export class AdminEtudiantService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/etudiants`;

  creerEtudiant(data: CreateEtudiantRequest): Observable<EtudiantAdmin> {
    return this.http.post<EtudiantAdmin>(this.apiUrl, data);
  }

  modifierEtudiant(
    id: number,
    data: UpdateEtudiantRequest,
  ): Observable<EtudiantAdmin> {
    return this.http.put<EtudiantAdmin>(`${this.apiUrl}/${id}`, data);
  }

  desactiverEtudiant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activerEtudiant(id: number): Observable<EtudiantAdmin> {
    return this.http.put<EtudiantAdmin>(`${this.apiUrl}/${id}/activation`, {});
  }

  listerEtudiants(
    options: {
      actif?: boolean;
      filiere?: string;
      page?: number;
      size?: number;
    } = {},
  ): Observable<EtudiantPageResponse> {
    let params = new HttpParams()
      .set('page', options.page ?? 0)
      .set('size', options.size ?? 10)
      .set('sort', 'nom,asc');

    if (options.actif !== undefined) {
      params = params.set('actif', options.actif);
    }
    if (options.filiere) {
      params = params.set('filiere', options.filiere);
    }

    return this.http.get<EtudiantPageResponse>(this.apiUrl, { params });
  }
}
