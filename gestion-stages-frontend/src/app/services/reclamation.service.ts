import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Reclamation, ReclamationDetail, MessageReclamation, TypeReclamation } from '../models/reclamation.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReclamationService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  creerReclamation(
    stageId: number,
    typeReclamation: TypeReclamation,
    objet: string,
    messageInitial: string
  ): Observable<Reclamation> {
    return this.http.post<Reclamation>(
      `${this.apiUrl}/stages/${stageId}/reclamations`,
      {
        typeReclamation,
        objet,
        messageInitial
      }
    );
  }

  listerReclamations(stageId: number): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(
      `${this.apiUrl}/stages/${stageId}/reclamations`
    );
  }

  listerReclamationsEntreprise(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(
      `${this.apiUrl}/entreprise/reclamations`
    );
  }

  getDetail(reclamationId: number): Observable<ReclamationDetail> {
    return this.http.get<ReclamationDetail>(
      `${this.apiUrl}/reclamations/${reclamationId}`
    );
  }

  ajouterMessage(
    reclamationId: number,
    contenu: string,
    pieceJointe?: string
  ): Observable<MessageReclamation> {
    return this.http.post<MessageReclamation>(
      `${this.apiUrl}/reclamations/${reclamationId}/messages`,
      {
        contenu,
        pieceJointe
      }
    );
  }

  resoudre(reclamationId: number): Observable<Reclamation> {
    return this.http.put<Reclamation>(
      `${this.apiUrl}/reclamations/${reclamationId}/resoudre`,
      {}
    );
  }

  cloturer(reclamationId: number): Observable<Reclamation> {
    return this.http.put<Reclamation>(
      `${this.apiUrl}/reclamations/${reclamationId}/cloturer`,
      {}
    );
  }

  rouvrir(reclamationId: number): Observable<Reclamation> {
    return this.http.put<Reclamation>(
      `${this.apiUrl}/reclamations/${reclamationId}/rouvrir`,
      {}
    );
  }
}
