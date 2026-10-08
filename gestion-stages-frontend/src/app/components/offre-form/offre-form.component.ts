import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ModeTravail, OffreStageRequest, StatutOffre } from '../../models/stage.model';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';

@Component({
  selector: 'app-offre-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="portal-page">
      <section class="portal-shell narrow">
        <a class="back-link" routerLink="/espace-entreprise">← Retour aux offres</a>
        <div class="card">
          <p class="eyebrow">Espace entreprise</p>
          <h1>{{ offreId ? "Modifier l'offre" : "Créer une offre" }}</h1>
          <p>Renseignez les informations qui seront visibles par les étudiants.</p>
          @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
          @if (loading) {
            <p>Chargement…</p>
          } @else {
            <form [formGroup]="form" (ngSubmit)="submit()">
              <div class="form-grid">
                <label class="full">Titre *<input formControlName="titre" /></label>
                <label class="full">Description *<textarea rows="7" formControlName="description"></textarea></label>
                <label>Domaine *<input formControlName="domaine" placeholder="Informatique, finance…" /></label>
                <label>Type de stage *<input formControlName="typeStage" placeholder="PFE, initiation…" /></label>
                <label>Localisation *<input formControlName="localisation" /></label>
                <label>Mode *<select formControlName="mode"><option value="PRESENTIEL">Présentiel</option><option value="HYBRIDE">Hybride</option><option value="DISTANCIEL">Distanciel</option></select></label>
                <label>Durée (mois) *<input type="number" min="1" formControlName="dureeMois" /></label>
                <label>Nombre de places *<input type="number" min="1" formControlName="nombrePlaces" /></label>
                <label>Date de début<input type="date" formControlName="dateDebut" /></label>
                <label>Date de fin<input type="date" formControlName="dateFin" /></label>
                <label>Date limite *<input type="date" formControlName="dateLimite" /></label>
                <label>Rémunération mensuelle<input type="number" min="0" formControlName="remuneration" /></label>
                <label>Niveau requis<input formControlName="niveauRequis" placeholder="Bac+3, Master…" /></label>
                <label>Statut<select formControlName="statut"><option value="BROUILLON">Brouillon</option><option value="PUBLIEE">Publiée</option><option value="CLOTUREE">Clôturée</option><option value="ARCHIVEE">Archivée</option></select></label>
                <label class="full">Compétences requises *<input formControlName="competences" placeholder="Angular, Java, SQL" /><small>Séparez les compétences par des virgules.</small></label>
              </div>
              @if (form.invalid && form.touched) { <p class="field-error">Veuillez compléter tous les champs obligatoires.</p> }
              <div class="form-actions">
                <a class="button secondary" routerLink="/espace-entreprise">Annuler</a>
                <button class="primary" type="submit" [disabled]="saving">{{ saving ? 'Enregistrement…' : 'Enregistrer' }}</button>
              </div>
            </form>
          }
        </div>
      </section>
    </main>
  `,
})
export class OffreFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EntrepriseStageService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly offreId = Number(this.route.snapshot.paramMap.get('id')) || null;
  loading = false;
  saving = false;
  errorMessage = '';

  readonly form = this.fb.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(180)]],
    description: ['', Validators.required],
    domaine: ['', Validators.required],
    typeStage: ['', Validators.required],
    localisation: ['', Validators.required],
    mode: this.fb.nonNullable.control<ModeTravail>('PRESENTIEL', Validators.required),
    dureeMois: [1, [Validators.required, Validators.min(1)]],
    nombrePlaces: [1, [Validators.required, Validators.min(1)]],
    dateDebut: [''],
    dateFin: [''],
    dateLimite: ['', Validators.required],
    remuneration: [0, Validators.min(0)],
    niveauRequis: [''],
    statut: ['PUBLIEE' as StatutOffre, Validators.required],
    competences: ['', Validators.required],
  });

  ngOnInit(): void {
    if (!this.offreId) return;
    this.loading = true;
    this.service.obtenirOffre(this.offreId).subscribe({
      next: (offre) => {
        this.form.patchValue({
          titre: offre.titre,
          description: offre.description,
          domaine: offre.domaine,
          typeStage: offre.typeStage,
          localisation: offre.localisation,
          mode: offre.mode,
          dureeMois: offre.dureeMois ?? 1,
          dateDebut: offre.dateDebut ?? '',
          dateLimite: offre.dateLimite,
          dateFin: offre.dateFin ?? '',
          remuneration: offre.remuneration ?? 0,
          niveauRequis: offre.niveauRequis ?? '',
          statut: offre.statut ?? 'PUBLIEE',
          nombrePlaces: offre.nombrePlaces ?? 1,
          competences: offre.competences.join(', '),
        });
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Impossible de charger l'offre.";
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = `Veuillez vérifier les champs suivants : ${this.invalidFieldLabels().join(', ')}.`;
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    const value = this.form.getRawValue();
    const request: OffreStageRequest = {
      ...value,
      competences: value.competences.split(',').map((item) => item.trim()).filter(Boolean),
      dateFin: value.dateFin || undefined,
      dateDebut: value.dateDebut || undefined,
      niveauRequis: value.niveauRequis || undefined,
      remuneration: value.remuneration || undefined,
    };
    const operation = this.offreId
      ? this.service.modifierOffre(this.offreId, request)
      : this.service.creerOffre(request);
    operation.subscribe({
      next: () => void this.router.navigate(['/espace-entreprise']),
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "L'offre n'a pas pu être enregistrée.";
        this.saving = false;
      },
    });
  }

  private invalidFieldLabels(): string[] {
    const labels: Record<string, string> = {
      titre: 'titre',
      description: 'description',
      domaine: 'domaine',
      typeStage: 'type de stage',
      localisation: 'localisation',
      mode: 'mode',
      dureeMois: 'durée',
      nombrePlaces: 'nombre de places',
      dateDebut: 'date de début',
      dateFin: 'date de fin',
      dateLimite: 'date limite',
      remuneration: 'rémunération',
      niveauRequis: 'niveau requis',
      statut: 'statut',
      competences: 'compétences',
    };

    return Object.entries(this.form.controls)
      .filter(([, control]) => control.invalid)
      .map(([name]) => labels[name] ?? name);
  }
}
