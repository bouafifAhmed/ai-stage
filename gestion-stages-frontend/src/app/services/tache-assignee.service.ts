import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  AssignerTacheRequest,
  DecisionTacheRequest,
  StatutTacheAssignee,
  TacheAssignee,
} from '../models/tache-assignee.model';

@Injectable({ providedIn: 'root' })
export class TacheAssigneeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/stages`;

  lister(stageId: number, statut?: StatutTacheAssignee): Observable<TacheAssignee[]> {
    const params = statut ? new HttpParams().set('statut', statut) : undefined;
    return this.http.get<TacheAssignee[]>(this.stageUrl(stageId), { params });
  }

  assigner(stageId: number, request: AssignerTacheRequest): Observable<TacheAssignee> {
    return this.http.post<TacheAssignee>(this.stageUrl(stageId), request);
  }

  terminer(
    stageId: number,
    tacheId: number,
    commentaireEtudiant: string,
    pieceJointe?: File,
  ): Observable<TacheAssignee> {
    const formData = new FormData();
    formData.append('commentaireEtudiant', commentaireEtudiant);
    if (pieceJointe) formData.append('pieceJointe', pieceJointe);
    return this.http.put<TacheAssignee>(
      `${this.stageUrl(stageId)}/${tacheId}/terminer`,
      formData,
    );
  }

  valider(
    stageId: number,
    tacheId: number,
    commentaireEncadrant = '',
  ): Observable<TacheAssignee> {
    const request: DecisionTacheRequest = { commentaireEncadrant };
    return this.http.put<TacheAssignee>(
      `${this.stageUrl(stageId)}/${tacheId}/valider`,
      request,
    );
  }

  rejeter(
    stageId: number,
    tacheId: number,
    commentaireEncadrant: string,
  ): Observable<TacheAssignee> {
    const request: DecisionTacheRequest = { commentaireEncadrant };
    return this.http.put<TacheAssignee>(
      `${this.stageUrl(stageId)}/${tacheId}/rejeter`,
      request,
    );
  }

  telechargerPieceJointe(stageId: number, tacheId: number): Observable<Blob> {
    return this.http.get(
      `${this.stageUrl(stageId)}/${tacheId}/piece-jointe`,
      { responseType: 'blob' },
    );
  }

  private stageUrl(stageId: number): string {
    return `${this.apiUrl}/${stageId}/taches-assignees`;
  }
}
