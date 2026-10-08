import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Candidature } from '../../models/stage.model';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';
import { TacheAssignee } from '../../models/tache-assignee.model';
import { TacheAssigneeService } from '../../services/tache-assignee.service';

@Component({
  selector: 'app-validation-taches-assignees',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav">
        <a class="brand" routerLink="/espace-entreprise">Gestion des stages</a>
        <nav>
          <a routerLink="/espace-entreprise">Mes offres</a>
          <a [routerLink]="['/espace-entreprise/stages', stageId, 'taches-assignees', 'assigner']">Assigner une tâche</a>
          <a [routerLink]="['/espace-entreprise/stages', stageId, 'calendrier']">Calendrier</a>
          <a [routerLink]="['/espace-entreprise/stages', stageId, 'cloture']">Clôture</a>
        </nav>
      </header>
      <div class="page-header">
        <div>
          <p class="eyebrow">Encadrement du stage</p>
          <h1>Tâches à valider</h1>
          @if (candidature) {
            <p>Stagiaire : <strong>{{ candidature.etudiantPrenom }} {{ candidature.etudiantNom }}</strong> · {{ candidature.offreTitre }}</p>
          } @else {
            <p>Contrôlez les travaux remis par l'étudiant en stage chez vous.</p>
          }
        </div>
        <a class="button primary" [routerLink]="['/espace-entreprise/stages', stageId, 'taches-assignees', 'assigner']">Assigner une tâche</a>
      </div>

      @if (errorMessage) { <div class="alert" role="alert">{{ errorMessage }}</div> }
      @if (successMessage) { <div class="success" role="status">{{ successMessage }}</div> }
      @if (loading) { <div class="card empty" aria-live="polite">Chargement des tâches…</div> }
      @else if (!taches.length) { <div class="card empty">Aucune tâche terminée n'attend de validation.</div> }
      @else {
        <div class="review-list">
          @for (tache of taches; track tache.id) {
            <article class="card">
              <div class="task-heading">
                <div><h2>{{ tache.titre }}</h2><p class="muted">Terminée le {{ tache.dateCompletion | date: 'dd/MM/yyyy à HH:mm' }}</p></div>
                <span class="badge status-terminee">Terminée</span>
              </div>
              <p class="description">{{ tache.description }}</p>
              <section class="submission" aria-label="Remise de l'étudiant">
                <h3>Remise de l'étudiant</h3>
                <p>{{ tache.commentaireEtudiant || 'Aucun commentaire.' }}</p>
                @if (hasAttachment(tache)) {
                  <button class="secondary download" type="button" (click)="download(tache)"
                    [disabled]="downloadingId === tache.id">
                    {{ downloadingId === tache.id ? 'Téléchargement…' : 'Télécharger la pièce jointe' }}
                  </button>
                } @else {
                  <p class="muted">Aucune pièce jointe.</p>
                }
              </section>
              <form class="decision-form" (ngSubmit)="decide(tache, pendingDecision[tache.id] || 'VALIDEE')" novalidate>
                <label [for]="'decision-' + tache.id">Commentaire de décision <small>(obligatoire pour un rejet)</small></label>
                <textarea [id]="'decision-' + tache.id" rows="4"
                  [required]="pendingDecision[tache.id] === 'REJETEE'"
                  [name]="'decision-' + tache.id" [(ngModel)]="comments[tache.id]"
                  [disabled]="savingId === tache.id"
                  placeholder="Expliquez votre validation ou les corrections attendues."></textarea>
                <div class="decision-actions">
                  <button class="reject-button" type="submit" (click)="pendingDecision[tache.id] = 'REJETEE'"
                    [disabled]="savingId === tache.id">
                    Rejeter
                  </button>
                  <button class="validate-button" type="submit" (click)="pendingDecision[tache.id] = 'VALIDEE'"
                    [disabled]="savingId === tache.id">
                    {{ savingId === tache.id ? 'Enregistrement…' : 'Valider' }}
                  </button>
                </div>
              </form>
            </article>
          }
        </div>
      }
    </section></main>
  `,
  styles: `
    .review-list { display: grid; gap: 1rem; }
    .task-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
    .task-heading h2 { margin: 0; }
    .task-heading p { margin: .35rem 0 0; }
    .status-terminee { color: #1e40af; background: #dbeafe; }
    .description { white-space: pre-line; line-height: 1.55; }
    .submission { margin: 1rem 0; padding: 1rem; border-radius: .6rem; background: #f8fafc; }
    .submission h3 { margin: 0 0 .5rem; }
    .submission p { white-space: pre-line; }
    .download { padding: 0 1rem; }
    .decision-form { display: grid; gap: .55rem; padding-top: 1rem; border-top: 1px solid #e2e8f0; }
    .decision-form textarea { width: 100%; padding: .7rem; border: 1px solid #94a3b8; border-radius: .5rem; resize: vertical; }
    .decision-actions { display: flex; justify-content: flex-end; gap: .75rem; }
    .reject-button, .validate-button { padding: 0 1rem; font-weight: 700; }
    .reject-button { border: 1px solid #dc2626; color: #b91c1c; background: #fff; }
    .validate-button { border: 0; color: #fff; background: #15803d; }
    button:disabled { opacity: .6; cursor: wait; }
    @media (max-width: 40rem) { .task-heading { flex-direction: column; } }
  `,
})
export class ValidationTachesAssigneesComponent implements OnInit {
  private readonly service = inject(TacheAssigneeService);
  private readonly stageService = inject(EntrepriseStageService);
  readonly stageId = Number(inject(ActivatedRoute).snapshot.paramMap.get('stageId'));
  candidature: Candidature | null = null;
  taches: TacheAssignee[] = [];
  comments: Record<number, string> = {};
  pendingDecision: Record<number, 'VALIDEE' | 'REJETEE'> = {};
  loading = true;
  savingId: number | null = null;
  downloadingId: number | null = null;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    this.stageService.obtenirCandidature(this.stageId).subscribe({
      next: (candidature) => { this.candidature = candidature; },
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.service.lister(this.stageId, 'TERMINEE').subscribe({
      next: (taches) => { this.taches = taches; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger les tâches à valider.';
        this.loading = false;
      },
    });
  }

  hasAttachment(tache: TacheAssignee): boolean {
    return !!tache.pieceJointeEtudiant;
  }

  decide(tache: TacheAssignee, decision: 'VALIDEE' | 'REJETEE'): void {
    const commentaire = this.comments[tache.id]?.trim();
    if (decision === 'REJETEE' && !commentaire) {
      this.errorMessage = 'Le motif du rejet est obligatoire.';
      return;
    }
    this.savingId = tache.id;
    this.errorMessage = '';
    this.successMessage = '';
    const operation = decision === 'VALIDEE'
      ? this.service.valider(this.stageId, tache.id, commentaire ?? '')
      : this.service.rejeter(this.stageId, tache.id, commentaire!);
    operation.subscribe({
      next: () => {
        this.taches = this.taches.filter((item) => item.id !== tache.id);
        delete this.comments[tache.id];
        delete this.pendingDecision[tache.id];
        this.successMessage = decision === 'VALIDEE'
          ? 'La tâche a été validée.'
          : 'La tâche a été rejetée et renvoyée à l’étudiant.';
        this.savingId = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "La décision n'a pas pu être enregistrée.";
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
}
