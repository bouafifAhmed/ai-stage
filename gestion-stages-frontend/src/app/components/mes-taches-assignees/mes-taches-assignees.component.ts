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

      <!-- Hero -->
      <div class="tasks-hero">
        <div class="tasks-hero-left">
          <span class="eyebrow">Suivi du stage</span>
          <h1>Mes tâches assignées</h1>
          <p class="hero-sub">Consultez et remettez les travaux demandés par votre encadrant.</p>
        </div>
        <div class="tasks-hero-right">
          <label class="filter-group" for="statut">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
            Filtrer
            <select id="statut" [(ngModel)]="selectedStatus" (ngModelChange)="load()">
              <option value="">Tous les statuts</option>
              @for (status of statuses; track status) {
                <option [value]="status">{{ statusLabel(status) }}</option>
              }
            </select>
          </label>
        </div>
      </div>

      @if (errorMessage) { <div class="alert" role="alert">{{ errorMessage }}</div> }
      @if (successMessage) { <div class="success" role="status">{{ successMessage }}</div> }

      @if (loading) {
        <div class="loading-state">
          <div class="loading-spinner"></div>
          <p>Chargement des tâches…</p>
        </div>
      }
      @else if (!taches.length) {
        <div class="empty-state">
          <div class="empty-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          </div>
          <p class="empty-title">Aucune tâche ne correspond</p>
          <p class="empty-hint">Essayez un autre filtre ou revenez plus tard.</p>
        </div>
      }
      @else {
        <div class="task-list">
          @for (tache of taches; track tache.id) {
            <article class="task-card" [class.card-validee]="tache.statut === 'VALIDEE'" [class.card-rejetee]="tache.statut === 'REJETEE'" [class.card-terminee]="tache.statut === 'TERMINEE'">
              <div class="task-heading">
                <div class="task-heading-info">
                  <h2>{{ tache.titre }}</h2>
                  <p class="muted">Assignée le {{ tache.dateAssignation | date: 'dd/MM/yyyy' }}</p>
                </div>
                <span class="task-badge" [class]="'task-badge status-' + tache.statut.toLowerCase()">
                  <span class="badge-dot"></span>
                  {{ statusLabel(tache.statut) }}
                </span>
              </div>

              <p class="description">{{ tache.description }}</p>

              <div class="task-meta">
                <span class="meta-item">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                  Échéance : {{ tache.dateEcheance ? (tache.dateEcheance | date: 'dd/MM/yyyy') : 'Non définie' }}
                </span>
              </div>

              @if (tache.commentaireEtudiant) {
                <div class="comment comment-student">
                  <p class="comment-label">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    Votre commentaire
                  </p>
                  <p>{{ tache.commentaireEtudiant }}</p>
                </div>
              }
              @if (tache.commentaireEncadrant) {
                <div class="comment comment-company">
                  <p class="comment-label">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                    Retour de l'entreprise
                  </p>
                  <p>{{ tache.commentaireEncadrant }}</p>
                </div>
              }

              @if (hasAttachment(tache)) {
                <button type="button" class="attachment-btn" (click)="download(tache)"
                  [disabled]="downloadingId === tache.id">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
                  {{ downloadingId === tache.id ? 'Téléchargement…' : 'Télécharger la pièce jointe' }}
                </button>
              }

              @if (canComplete(tache)) {
                <form class="completion-form" (ngSubmit)="complete(tache)" novalidate>
                  <p class="form-section-title">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
                    Marquer comme terminée
                  </p>
                  <label [for]="'comment-' + tache.id">Commentaire de remise <span class="required">*</span>
                    <textarea [id]="'comment-' + tache.id" rows="3" required
                      [name]="'comment-' + tache.id" [(ngModel)]="comments[tache.id]"
                      [disabled]="savingId === tache.id"
                      placeholder="Décrivez ce que vous avez réalisé..."></textarea>
                  </label>
                  <label [for]="'file-' + tache.id">Pièce jointe <span class="optional">(optionnelle)</span>
                    <input [id]="'file-' + tache.id" type="file"
                      (change)="selectFile(tache.id, $event)" [disabled]="savingId === tache.id" />
                  </label>
                  <div class="form-actions-row">
                    <button class="btn-submit primary" type="submit" [disabled]="savingId === tache.id">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
                      {{ savingId === tache.id ? 'Envoi…' : 'Soumettre la tâche' }}
                    </button>
                  </div>
                </form>
              }
            </article>
          }
        </div>
      }
    </section></main>
  `,
  styles: `
    /* ── Hero ── */
    .tasks-hero {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1.25rem;
      margin-bottom: 2rem;
      padding: 1.75rem 2rem;
      background: linear-gradient(135deg, rgba(37,99,235,0.05) 0%, rgba(6,182,212,0.04) 100%);
      border: 1px solid rgba(37,99,235,0.1);
      border-radius: 1.25rem;
    }
    .tasks-hero-left { flex: 1; min-width: 200px; }
    .tasks-hero-left h1 {
      margin: 0.25rem 0 0.4rem;
      font-size: 1.75rem;
      font-weight: 800;
      letter-spacing: -0.025em;
      color: var(--text-main);
    }
    .hero-sub { margin: 0; color: var(--text-muted); font-size: 0.92rem; }
    .tasks-hero-right { display: flex; align-items: center; }

    /* Filter */
    .filter-group {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 0.75rem;
      background: #fff;
      border: 1.5px solid #e2e8f0;
      border-radius: 0.75rem;
      font-size: 0.875rem;
      font-weight: 700;
      color: var(--text-secondary);
      cursor: pointer;
      transition: border-color 0.15s;
    }
    .filter-group:focus-within {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(37,99,235,0.12);
    }
    .filter-group select {
      border: none;
      background: transparent;
      font-weight: 700;
      font-size: 0.875rem;
      color: var(--text-main);
      padding: 0;
      cursor: pointer;
      min-height: unset;
      margin: 0;
      width: auto;
    }
    .filter-group select:focus { outline: none; box-shadow: none; }

    /* ── Task list ── */
    .task-list { display: grid; gap: 1.25rem; }

    .task-card {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 1rem;
      padding: 1.5rem 1.75rem;
      border-left: 4px solid #e2e8f0;
      box-shadow: 0 2px 8px -1px rgba(15,23,42,0.05);
      transition: box-shadow 0.2s, transform 0.2s;
    }
    .task-card:hover {
      box-shadow: 0 8px 24px -4px rgba(15,23,42,0.09);
      transform: translateY(-1px);
    }
    .card-validee { border-left-color: #10b981; }
    .card-rejetee { border-left-color: #ef4444; }
    .card-terminee { border-left-color: #3b82f6; }

    /* Heading */
    .task-heading {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 0.75rem;
      flex-wrap: wrap;
    }
    .task-heading-info h2 { margin: 0; font-size: 1.05rem; font-weight: 700; color: #1e293b; letter-spacing: -0.01em; }
    .task-heading-info .muted { margin: 0.25rem 0 0; font-size: 0.85rem; color: #64748b; }

    /* Status badges */
    .task-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.28rem 0.75rem;
      border-radius: 999px;
      font-size: 0.74rem;
      font-weight: 700;
      letter-spacing: 0.02em;
      white-space: nowrap;
      flex-shrink: 0;
    }
    .badge-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
    .status-assignee { color: #475569; background: #f1f5f9; border: 1px solid #e2e8f0; }
    .status-assignee .badge-dot { background: #94a3b8; }
    .status-en_cours { color: #92400e; background: #fef3c7; border: 1px solid rgba(146,64,14,0.15); }
    .status-en_cours .badge-dot { background: #f59e0b; }
    .status-terminee { color: #1e40af; background: #dbeafe; border: 1px solid rgba(30,64,175,0.15); }
    .status-terminee .badge-dot { background: #3b82f6; }
    .status-validee { color: #15803d; background: #dcfce7; border: 1px solid rgba(21,128,61,0.15); }
    .status-validee .badge-dot { background: #10b981; }
    .status-rejetee { color: #991b1b; background: #fee2e2; border: 1px solid rgba(153,27,27,0.15); }
    .status-rejetee .badge-dot { background: #ef4444; }

    /* Description & meta */
    .description { white-space: pre-line; line-height: 1.65; color: #475569; font-size: 0.92rem; margin: 0 0 0.75rem; }
    .task-meta { display: flex; gap: 1.25rem; flex-wrap: wrap; margin-bottom: 0.5rem; }
    .meta-item {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.85rem;
      font-weight: 600;
      color: #64748b;
    }

    /* Comments */
    .comment {
      margin-top: 0.85rem;
      padding: 0.75rem 1rem;
      border-radius: 0.5rem;
      border-left: 3px solid #94a3b8;
      background: #f8fafc;
    }
    .comment-company { border-left-color: #0369a1; background: #f0f9ff; }
    .comment-label {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.8rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #475569;
      margin: 0 0 0.35rem;
    }
    .comment-company .comment-label { color: #0369a1; }
    .comment p:last-child { margin: 0; font-size: 0.9rem; white-space: pre-line; color: #334155; line-height: 1.55; }

    /* Attachment button */
    .attachment-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      margin-top: 0.85rem;
      color: #2563eb;
      background: #eff6ff;
      border: 1px solid rgba(37,99,235,0.15);
      padding: 0.4rem 0.85rem;
      border-radius: 0.5rem;
      font-weight: 600;
      font-size: 0.875rem;
      cursor: pointer;
      transition: all 0.15s;
    }
    .attachment-btn:hover:not([disabled]) { background: #dbeafe; border-color: rgba(37,99,235,0.3); }
    .attachment-btn[disabled] { opacity: 0.6; cursor: not-allowed; }

    /* Completion form */
    .completion-form {
      display: grid;
      gap: 0.75rem;
      margin-top: 1.25rem;
      padding-top: 1.25rem;
      border-top: 1px dashed #e2e8f0;
    }
    .form-section-title {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.875rem;
      font-weight: 800;
      color: var(--primary, #2563eb);
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .completion-form label { font-size: 0.875rem; font-weight: 700; color: #334155; }
    .completion-form textarea,
    .completion-form input[type='file'] {
      width: 100%;
      margin-top: 0.35rem;
      padding: 0.65rem 0.85rem;
      border: 1.5px solid #cbd5e1;
      border-radius: 0.6rem;
      font-size: 0.9rem;
      color: #1e293b;
      background: #f8fafc;
      transition: border-color 0.15s, box-shadow 0.15s;
    }
    .completion-form textarea { resize: vertical; }
    .completion-form textarea:focus,
    .completion-form input[type='file']:focus {
      outline: none;
      border-color: var(--primary, #2563eb);
      background: #fff;
      box-shadow: 0 0 0 3px rgba(37,99,235,0.1);
    }
    .required { color: #ef4444; font-weight: 800; }
    .optional { color: #94a3b8; font-size: 0.8rem; font-weight: 600; }
    .form-actions-row { display: flex; justify-content: flex-end; }
    .btn-submit {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
    }

    /* Loading */
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 4rem;
      color: #64748b;
    }
    .loading-spinner {
      width: 32px;
      height: 32px;
      border: 3px solid #e2e8f0;
      border-top-color: #2563eb;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Empty state */
    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
      background: #fff;
      border: 1.5px dashed #cbd5e1;
      border-radius: 1.25rem;
    }
    .empty-icon {
      width: 68px;
      height: 68px;
      margin: 0 auto 1.25rem;
      background: linear-gradient(135deg, #eff6ff, #e0f2fe);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #2563eb;
    }
    .empty-title { font-size: 1.1rem; font-weight: 700; color: #1e293b; margin: 0 0 0.4rem; }
    .empty-hint { color: #64748b; font-size: 0.9rem; margin: 0; }
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
