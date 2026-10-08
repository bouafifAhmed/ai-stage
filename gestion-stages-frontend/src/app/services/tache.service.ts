import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tache, Progression } from '../models/tache.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TacheService {
  private apiUrl = `${environment.apiUrl}/stages`; 

  constructor(private http: HttpClient) {}

  creerTache(stageId: number, data: Partial<Tache>): Observable<Tache> {
    return this.http.post<Tache>(`${this.apiUrl}/${stageId}/journal`, data);
  }

  modifierTache(stageId: number, tacheId: number, data: Partial<Tache>): Observable<Tache> {
    return this.http.put<Tache>(`${this.apiUrl}/${stageId}/journal/${tacheId}`, data);
  }

  listerTaches(stageId: number): Observable<Tache[]> {
    return this.http.get<Tache[]>(`${this.apiUrl}/${stageId}/journal`);
  }

  getProgression(stageId: number): Observable<Progression> {
    return this.http.get<Progression>(`${this.apiUrl}/${stageId}/journal/progression`);
  }

  uploaderFichier(stageId: number, file: File): Observable<{ chemin: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ chemin: string }>(`${this.apiUrl}/${stageId}/journal/upload`, formData);
  }

  telechargerPieceJointe(stageId: number, tacheId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${stageId}/journal/${tacheId}/piece-jointe`, {
      responseType: 'blob',
    });
  }

  telechargerJournalPdf(stageId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${stageId}/journal/pdf`, {
      responseType: 'blob',
    });
  }
}

