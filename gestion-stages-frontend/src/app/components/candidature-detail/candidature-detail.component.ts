import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Candidature, StatutCandidature } from '../../models/stage.model';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';

@Component({
  selector: 'app-candidature-detail',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell narrow">
      <a class="back-link" routerLink="/espace-entreprise">← Retour aux offres</a>
      @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
      @if (successMessage) { <div class="success">{{ successMessage }}</div> }
      @if (loading) { <div class="card">Chargement…</div> }
      @else if (candidature) {
        <article class="card">
          <p class="eyebrow">Détail de la candidature</p>
          <div class="page-header"><div><h1>{{ candidature.etudiantPrenom }} {{ candidature.etudiantNom }}</h1><p>{{ candidature.etudiantEmail }}</p></div><span class="badge">{{ candidature.statut }}</span></div>
          <dl class="details-grid">
            <div><dt>Offre</dt><dd>{{ candidature.offreTitre || ('Offre #' + candidature.offreId) }}</dd></div>
            <div><dt>Date de candidature</dt><dd>{{ candidature.dateCandidature | date: 'dd/MM/yyyy à HH:mm' }}</dd></div>
            <div><dt>Téléphone</dt><dd>{{ candidature.telephone || 'Non renseigné' }}</dd></div>
            <div><dt>Formation</dt><dd>{{ candidature.filiere || 'Non renseignée' }} · {{ candidature.niveauEtudes || 'Niveau non renseigné' }}</dd></div>
          </dl>
          <h2>Compétences</h2>
          <div class="tags">
            @for (competence of candidature.competences || []; track competence) { <span>{{ competence }}</span> }
            @if (!candidature.competences?.length) { <p>Aucune compétence renseignée.</p> }
          </div>
          <h2>Message de motivation</h2>
          <p class="message">{{ candidature.message }}</p>
          <section class="decision">
            <div><h2>Décision</h2><p class="muted">Acceptez ou refusez cette candidature en un clic.</p></div>
            <div class="decision-actions">
              <button type="button" class="primary" (click)="updateStatus('ACCEPTEE')" [disabled]="updating !== null || candidature.statut === 'ACCEPTEE'">
                {{ updating === 'ACCEPTEE' ? 'Acceptation…' : 'Accepter' }}
              </button>
              <button type="button" class="danger-action" (click)="updateStatus('REFUSEE')" [disabled]="updating !== null || candidature.statut === 'REFUSEE'">
                {{ updating === 'REFUSEE' ? 'Refus…' : 'Refuser' }}
              </button>
            </div>
          </section>
          @if (candidature.statut === 'ACCEPTEE') {
            <section class="stage-actions">
              <h2>Encadrement du stagiaire</h2>
              <p class="muted">Assignez des tâches à {{ candidature.etudiantPrenom }} {{ candidature.etudiantNom }} pendant son stage chez vous.</p>
              <div class="stage-links">
                <a class="button primary" [routerLink]="['/espace-entreprise/stages', candidature.id, 'taches-assignees', 'assigner']">Assigner une tâche</a>
                <a class="button secondary" [routerLink]="['/espace-entreprise/stages', candidature.id, 'taches-assignees', 'validation']">Valider les tâches remises</a>
                <a class="button secondary" [routerLink]="['/espace-entreprise/stages', candidature.id, 'calendrier']">Calendrier</a>
                <a class="button secondary" [routerLink]="['/espace-entreprise/stages', candidature.id, 'cloture']">Clôture & PDF</a>
              </div>
            </section>
          }
        </article>
      }
    </section></main>
  `,
  styles: `
    .decision { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid #e2e8f0; }
    .decision h2, .decision p { margin: 0; }
    .decision p { margin-top: .3rem; }
    .decision-actions { display: flex; gap: .75rem; }
    .danger-action { padding: 0 1rem; border: 1px solid #dc2626; color: #b91c1c; background: #fff; font-weight: 700; }
    .danger-action:disabled { opacity: .55; cursor: not-allowed; }
    .stage-actions { margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid #e2e8f0; }
    .stage-actions h2 { margin: 0 0 .35rem; }
    .stage-links { display: flex; flex-wrap: wrap; gap: .75rem; margin-top: 1rem; }
    .stage-links .secondary { padding: 0 1rem; }
    @media (max-width: 40rem) { .decision, .decision-actions { align-items: stretch; flex-direction: column; } }
  `,
})
export class CandidatureDetailComponent implements OnInit {
  private readonly service = inject(EntrepriseStageService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  candidature: Candidature | null = null;
  loading = true;
  errorMessage = '';
  successMessage = '';
  updating: StatutCandidature | null = null;

  ngOnInit(): void {
    this.service.obtenirCandidature(this.id).subscribe({
      next: (item) => { this.candidature = item; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger la candidature.';
        this.loading = false;
      },
    });
  }

  updateStatus(statut: Exclude<StatutCandidature, 'EN_ATTENTE'>): void {
    if (!this.candidature || this.updating) return;
    this.updating = statut;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.modifierStatutCandidature(this.candidature.id, statut).subscribe({
      next: (candidature) => {
        this.candidature = candidature;
        this.successMessage = statut === 'ACCEPTEE'
          ? 'La candidature a été acceptée.'
          : 'La candidature a été refusée.';
        this.updating = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "La décision n'a pas pu être enregistrée.";
        this.updating = null;
      },
    });
  }
}
