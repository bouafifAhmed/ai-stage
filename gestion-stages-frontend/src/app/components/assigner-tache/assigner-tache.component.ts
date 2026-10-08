import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Candidature } from '../../models/stage.model';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';
import { TacheAssigneeService } from '../../services/tache-assignee.service';

@Component({
  selector: 'app-assigner-tache',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="portal-page">
      <section class="portal-shell narrow">
        <a class="back-link" routerLink="/espace-entreprise">← Retour aux offres</a>
        <article class="card">
          <p class="eyebrow">Encadrement du stage</p>
          <h1>Assigner une tâche</h1>

          @if (loading) {
            <p>Chargement du stagiaire…</p>
          } @else if (candidature) {
            <section class="stage-context">
              <p><strong>Stagiaire :</strong> {{ candidature.etudiantPrenom }} {{ candidature.etudiantNom }}</p>
              <p><strong>Offre :</strong> {{ candidature.offreTitre || ('Offre #' + candidature.offreId) }}</p>
              <p class="muted">La tâche sera visible par cet étudiant dans son espace « Mes tâches ».</p>
            </section>
          }

          @if (errorMessage) { <div class="alert" role="alert">{{ errorMessage }}</div> }
          @if (successMessage) { <div class="success" role="status">{{ successMessage }}</div> }

          @if (candidature?.statut === 'ACCEPTEE') {
            <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
              <div class="form-grid">
                <label class="full" for="titre">Titre *</label>
                <input class="full field" id="titre" formControlName="titre" maxlength="150"
                  [attr.aria-invalid]="form.controls.titre.invalid && form.controls.titre.touched" />
                @if (form.controls.titre.invalid && form.controls.titre.touched) {
                  <small class="field-error full">Le titre est obligatoire.</small>
                }

                <label class="full" for="description">Description *</label>
                <textarea class="full field" id="description" rows="7" maxlength="20000" formControlName="description"
                  [attr.aria-invalid]="form.controls.description.invalid && form.controls.description.touched"></textarea>
                @if (form.controls.description.invalid && form.controls.description.touched) {
                  <small class="field-error full">La description est obligatoire.</small>
                }

                <label for="dateEcheance">Date d'échéance</label>
                <input class="field" id="dateEcheance" type="date" formControlName="dateEcheance" />
              </div>
              <div class="form-actions">
                <a class="button secondary" [routerLink]="['/espace-entreprise/stages', stageId, 'taches-assignees', 'validation']">Voir les tâches remises</a>
                <button class="primary" type="submit" [disabled]="saving">
                  {{ saving ? 'Assignation…' : 'Assigner au stagiaire' }}
                </button>
              </div>
            </form>
          } @else if (!loading && candidature) {
            <p class="alert">Seule une candidature acceptée permet d'assigner des tâches à l'étudiant.</p>
          }
        </article>
      </section>
    </main>
  `,
  styles: `
    .stage-context { margin: 1rem 0 1.5rem; padding: 1rem; border-radius: .6rem; background: #f0f9ff; }
    .stage-context p { margin: 0 0 .35rem; }
    .stage-context p:last-child { margin-bottom: 0; }
    .field { width: 100%; padding: .7rem .75rem; border: 1px solid #94a3b8; border-radius: .5rem; }
    textarea.field { resize: vertical; }
    label.full { margin-bottom: -.7rem; }
    .form-actions { display: flex; flex-wrap: wrap; gap: .75rem; justify-content: flex-end; }
    .form-actions .secondary { padding: 0 1rem; }
  `,
})
export class AssignerTacheComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(TacheAssigneeService);
  private readonly stageService = inject(EntrepriseStageService);
  readonly stageId = Number(inject(ActivatedRoute).snapshot.paramMap.get('stageId'));
  candidature: Candidature | null = null;
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';

  readonly form = this.fb.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(20000)]],
    dateEcheance: [''],
  });

  ngOnInit(): void {
    this.stageService.obtenirCandidature(this.stageId).subscribe({
      next: (candidature) => {
        this.candidature = candidature;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger le stagiaire.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Veuillez compléter tous les champs obligatoires.';
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const value = this.form.getRawValue();
    this.service.assigner(this.stageId, {
      ...value,
      dateEcheance: value.dateEcheance || null,
    }).subscribe({
      next: () => {
        this.successMessage = `La tâche a été assignée à ${this.candidature?.etudiantPrenom} ${this.candidature?.etudiantNom}.`;
        this.form.reset();
        this.saving = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "La tâche n'a pas pu être assignée.";
        this.saving = false;
      },
    });
  }
}
