import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import {
  StatutTacheAssignee,
  TacheAssignee,
} from '../../models/tache-assignee.model';
import { TacheAssigneeService } from '../../services/tache-assignee.service';

@Component({
  selector: 'app-mes-taches-assignees',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav">
        <a class="brand" routerLink="/espace-etudiant">Gestion des stages</a>
        <nav><a routerLink="/espace-etudiant">Offres</a><a routerLink="/espace-etudiant/candidatures">Mes candidatures</a><a routerLink="/espace-etudiant/notifications">Notifications</a><a routerLink="/espace-etudiant/profil">Mon profil</a><a [routerLink]="['/espace-etudiant/stages', stageId, 'calendrier']">Calendrier</a><a [routerLink]="['/espace-etudiant/stages', stageId, 'cloture']">Clôture</a></nav>
      </header>
      <div class="page-header">
        <div><p class="eyebrow">Suivi du stage</p><h1>Mes tâches assignées</h1><p>Consultez et remettez les travaux demandés.</p></div>
        <label class="filter" for="statut">Filtrer par statut
          <select id="statut" [(ngModel)]="selectedStatus" (ngModelChange)="load()">
            <option value="">Tous les statuts</option>
            @for (status of statuses; track status) {
              <option [value]="status">{{ statusLabel(status) }}</option>
            }
          </select>
        </label>
      </div>

      @if (errorMessage) { <div class="alert" role="alert">{{ errorMessage }}</div> }
      @if (successMessage) { <div class="success" role="status">{{ successMessage }}</div> }
      @if (loading) { <div class="card empty" aria-live="polite">Chargement des tâches…</div> }
      @else if (!taches.length) { <div class="card empty">Aucune tâche ne correspond à ce filtre.</div> }
      @else {
        <div class="task-list">
          @for (tache of taches; track tache.id) {
            <article class="card task-card">
              <div class="task-heading">
                <div><h2>{{ tache.titre }}</h2><p class="muted">Assignée le {{ tache.dateAssignation | date: 'dd/MM/yyyy' }}</p></div>
                <span class="task-badge" [class]="'task-badge status-' + tache.statut.toLowerCase()">{{ statusLabel(tache.statut) }}</span>
              </div>
              <p class="description">{{ tache.description }}</p>
              <p><strong>Échéance :</strong> {{ tache.dateEcheance ? (tache.dateEcheance | date: 'dd/MM/yyyy') : 'Non définie' }}</p>
              @if (tache.commentaireEtudiant) {
                <div class="comment"><strong>Votre commentaire</strong><p>{{ tache.commentaireEtudiant }}</p></div>
              }
              @if (tache.commentaireEncadrant) {
                <div class="comment company-comment"><strong>Retour de l'entreprise</strong><p>{{ tache.commentaireEncadrant }}</p></div>
              }
              @if (hasAttachment(tache)) {
                <button type="button" class="link-button attachment" (click)="download(tache)"
                  [disabled]="downloadingId === tache.id">
                  {{ downloadingId === tache.id ? 'Téléchargement…' : 'Télécharger la pièce jointe' }}
                </button>
              }
              @if (canComplete(tache)) {
                <form class="completion-form" (ngSubmit)="complete(tache)" novalidate>
                  <label [for]="'comment-' + tache.id">Commentaire de remise *</label>
                  <textarea [id]="'comment-' + tache.id" rows="4" required
                    [name]="'comment-' + tache.id" [(ngModel)]="comments[tache.id]"
                    [disabled]="savingId === tache.id"></textarea>
                  <label [for]="'file-' + tache.id">Pièce jointe <small>(optionnelle)</small></label>
                  <input [id]="'file-' + tache.id" type="file"
                    (change)="selectFile(tache.id, $event)" [disabled]="savingId === tache.id" />
                  <button class="primary" type="submit" [disabled]="savingId === tache.id">
                    {{ savingId === tache.id ? 'Envoi…' : 'Marquer comme terminée' }}
                  </button>
                </form>
              }
            </article>
          }
        </div>
      }
    </section></main>
  `,
  styles: `
    .filter { min-width: 14rem; }
    .filter select { display: block; width: 100%; margin-top: .35rem; padding: .6rem; border: 1px solid #94a3b8; border-radius: .5rem; }
    .task-list { display: grid; gap: 1rem; }
    .task-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
    .task-heading h2 { margin: 0; }
    .task-heading p { margin: .35rem 0 0; }
    .task-badge { display: inline-block; padding: .35rem .7rem; border-radius: 999px; font-size: .8rem; font-weight: 800; white-space: nowrap; }
    .status-assignee { color: #475569; background: #e2e8f0; }
    .status-en_cours { color: #92400e; background: #fef3c7; }
    .status-terminee { color: #1e40af; background: #dbeafe; }
    .status-validee { color: #166534; background: #dcfce7; }
    .status-rejetee { color: #991b1b; background: #fee2e2; }
    .description { white-space: pre-line; line-height: 1.55; }
    .comment { margin-top: 1rem; padding: .8rem 1rem; border-left: .25rem solid #64748b; background: #f8fafc; }
    .comment p { margin: .35rem 0 0; white-space: pre-line; }
    .company-comment { border-color: #0369a1; background: #f0f9ff; }
    .attachment { margin-top: 1rem; font-weight: 700; }
    .completion-form { display: grid; gap: .55rem; margin-top: 1.25rem; padding-top: 1.25rem; border-top: 1px solid #e2e8f0; }
    .completion-form textarea, .completion-form input { width: 100%; padding: .7rem; border: 1px solid #94a3b8; border-radius: .5rem; }
    .completion-form textarea { resize: vertical; }
    .completion-form button { justify-self: end; padding: 0 1rem; }
    @media (max-width: 40rem) { .task-heading { flex-direction: column; } }
  `,
})
export class MesTachesAssigneesComponent implements OnInit {
  private readonly service = inject(TacheAssigneeService);
  readonly stageId = Number(inject(ActivatedRoute).snapshot.paramMap.get('stageId'));
  readonly statuses: StatutTacheAssignee[] = ['ASSIGNEE', 'EN_COURS', 'TERMINEE', 'VALIDEE', 'REJETEE'];
  taches: TacheAssignee[] = [];
  selectedStatus: StatutTacheAssignee | '' = '';
  comments: Record<number, string> = {};
  files: Record<number, File | undefined> = {};
  loading = true;
  savingId: number | null = null;
  downloadingId: number | null = null;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.lister(this.stageId, this.selectedStatus || undefined).subscribe({
      next: (taches) => { this.taches = taches; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger les tâches.';
        this.loading = false;
      },
    });
  }

  canComplete(tache: TacheAssignee): boolean {
    return tache.statut === 'ASSIGNEE' || tache.statut === 'EN_COURS';
  }

  hasAttachment(tache: TacheAssignee): boolean {
    return !!tache.pieceJointeEtudiant;
  }

  selectFile(tacheId: number, event: Event): void {
    this.files[tacheId] = (event.target as HTMLInputElement).files?.[0];
  }

  complete(tache: TacheAssignee): void {
    const commentaire = this.comments[tache.id]?.trim();
    if (!commentaire) {
      this.errorMessage = 'Le commentaire de remise est obligatoire.';
      return;
    }
    this.savingId = tache.id;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.terminer(this.stageId, tache.id, commentaire, this.files[tache.id]).subscribe({
      next: (updated) => {
        this.taches = this.taches.map((item) => item.id === updated.id ? updated : item);
        delete this.comments[tache.id];
        delete this.files[tache.id];
        this.successMessage = 'La tâche a été remise avec succès.';
        this.savingId = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "La tâche n'a pas pu être remise.";
        this.savingId = null;
      },
    });
  }

  download(tache: TacheAssignee): void {
    this.downloadingId = tache.id;
    this.errorMessage = '';
    this.service.telechargerPieceJointe(this.stageId, tache.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = tache.pieceJointeEtudiant || `piece-jointe-tache-${tache.id}`;
        link.click();
        URL.revokeObjectURL(url);
        this.downloadingId = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de télécharger la pièce jointe.';
        this.downloadingId = null;
      },
    });
  }

  statusLabel(status: StatutTacheAssignee): string {
    return {
      ASSIGNEE: 'Assignée',
      EN_COURS: 'En cours',
      TERMINEE: 'Terminée',
      VALIDEE: 'Validée',
      REJETEE: 'Rejetée',
    }[status];
  }
}
