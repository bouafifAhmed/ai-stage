import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { CvExtraction } from '../../models/profil-etudiant.model';
import { EtudiantStageService } from '../../services/etudiant-stage.service';

@Component({
  selector: 'app-profil-etudiant',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell narrow">
      <a class="back-link" routerLink="/espace-etudiant">← Retour aux offres</a>
      <div class="card">
        <p class="eyebrow">Espace étudiant</p><h1>Mon profil</h1>
        <p>Ces informations accompagneront vos candidatures.</p>
        @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
        @if (successMessage) { <div class="success">{{ successMessage }}</div> }
        @if (loading) { <p>Chargement…</p> }
        @else {
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-grid">
              <label>Téléphone *<input formControlName="telephone" /></label>
              <label>Filière *<input formControlName="filiere" /></label>
              <label>Niveau *<input formControlName="niveauEtudes" /></label>
              <label class="full">Compétences *<input formControlName="competences" placeholder="Angular, Java, SQL" /><small>Séparez par des virgules.</small></label>
            </div>
            <div class="form-actions"><a class="button secondary" routerLink="/espace-etudiant">Annuler</a><button class="primary" [disabled]="saving">{{ saving ? 'Enregistrement…' : 'Enregistrer mon profil' }}</button></div>
          </form>
          <section class="cv-section" aria-labelledby="cv-title">
            <div>
              <p class="eyebrow">Document de candidature</p>
              <h2 id="cv-title">Mon CV détaillé</h2>
              <p class="muted">Déposez un seul fichier PDF de 20 Mo maximum. Un nouveau dépôt remplacera le CV actuel.</p>
            </div>

            @if (cvPresent) {
              <div class="cv-current">
                <div>
                  <strong>{{ cvNomFichier }}</strong>
                  @if (cvDateDepot) {
                    <small>Déposé le {{ cvDateDepot | date:'dd/MM/yyyy à HH:mm' }}</small>
                  }
                </div>
                <div class="cv-actions">
                  <button type="button" class="secondary" (click)="viewCv()" [disabled]="previewingCv">
                    {{ previewingCv ? 'Ouverture…' : 'Voir' }}
                  </button>
                  <button type="button" class="secondary" (click)="extractCv()" [disabled]="extractingCv">
                    {{ extractingCv ? 'Analyse…' : 'Préremplir le profil' }}
                  </button>
                  <button type="button" class="secondary" (click)="downloadCv()" [disabled]="downloadingCv">
                    {{ downloadingCv ? 'Téléchargement…' : 'Télécharger' }}
                  </button>
                  <button type="button" class="link-button danger-text" (click)="deleteCv()" [disabled]="deletingCv">
                    {{ deletingCv ? 'Suppression…' : 'Supprimer' }}
                  </button>
                </div>
              </div>
            } @else {
              <p class="empty-cv">Aucun CV déposé pour le moment.</p>
            }

            <label class="cv-picker">
              Sélectionner un CV au format PDF
              <input type="file" accept=".pdf,application/pdf" (change)="onCvSelected($event)" />
            </label>
            @if (selectedCv) {
              <div class="cv-selection">
                <span>{{ selectedCv.name }} ({{ selectedCv.size / 1024 / 1024 | number:'1.1-2' }} Mo)</span>
                <button type="button" class="primary" (click)="uploadCv()" [disabled]="uploadingCv">
                  {{ uploadingCv ? 'Envoi…' : (cvPresent ? 'Remplacer mon CV' : 'Déposer mon CV') }}
                </button>
              </div>
            }
          </section>
          @if (cvPreviewUrl) {
            <div class="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="cv-preview-title" (click)="closeCvPreview()">
              <section class="modal cv-preview" (click)="$event.stopPropagation()">
                <div class="cv-preview-header">
                  <h2 id="cv-preview-title">{{ cvNomFichier }}</h2>
                  <button type="button" class="secondary" (click)="closeCvPreview()">Fermer</button>
                </div>
                <iframe [src]="cvPreviewUrl" title="Aperçu de votre CV"></iframe>
              </section>
            </div>
          }
        }
      </div>
    </section></main>
  `,
  styles: `
    .cv-section { margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid #e2e8f0; }
    .cv-section h2 { margin: 0; }
    .cv-current, .cv-selection { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin: 1rem 0; padding: 1rem; border-radius: .65rem; background: #f8fafc; }
    .cv-current small { display: block; margin-top: .25rem; color: #64748b; }
    .cv-actions { display: flex; align-items: center; gap: .8rem; }
    .cv-actions .secondary { padding: 0 1rem; border: 1px solid #94a3b8; background: #fff; }
    .cv-picker { display: block; margin-top: 1rem; }
    .cv-picker input { display: block; width: 100%; margin-top: .5rem; padding: .75rem; border: 1px dashed #94a3b8; border-radius: .5rem; background: #f8fafc; }
    .empty-cv { margin: 1rem 0; padding: 1rem; color: #64748b; background: #f8fafc; border-radius: .65rem; }
    .cv-preview { width: min(95vw, 70rem); height: min(90vh, 55rem); display: flex; flex-direction: column; }
    .cv-preview-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }
    .cv-preview-header h2 { margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .cv-preview-header .secondary { padding: 0 1rem; border: 1px solid #94a3b8; background: #fff; }
    .cv-preview iframe { flex: 1; width: 100%; border: 1px solid #cbd5e1; border-radius: .5rem; background: #fff; }
    @media (max-width: 40rem) { .cv-current, .cv-selection, .cv-actions { align-items: stretch; flex-direction: column; } }
  `,
})
export class ProfilEtudiantComponent implements OnInit, OnDestroy {
  private readonly service = inject(EtudiantStageService);
  private readonly fb = inject(FormBuilder);
  private readonly sanitizer = inject(DomSanitizer);
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';
  selectedCv: File | null = null;
  cvPresent = false;
  cvNomFichier = '';
  cvDateDepot?: string;
  uploadingCv = false;
  downloadingCv = false;
  previewingCv = false;
  extractingCv = false;
  deletingCv = false;
  cvPreviewUrl?: SafeResourceUrl;
  private cvInput?: HTMLInputElement;
  private cvPreviewObjectUrl?: string;
  readonly form = this.fb.nonNullable.group({
    telephone: ['', Validators.required],
    filiere: ['', Validators.required],
    niveauEtudes: ['', Validators.required],
    competences: ['', Validators.required],
  });

  ngOnInit(): void {
    this.service.obtenirProfil().subscribe({
      next: (profil) => {
        this.form.patchValue({ ...profil, competences: profil.competences.join(', ') });
        this.updateCvState(profil);
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger votre profil.';
        this.loading = false;
      },
    });
  }

  ngOnDestroy(): void {
    this.releaseCvPreview();
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true; this.errorMessage = ''; this.successMessage = '';
    const value = this.form.getRawValue();
    this.service.modifierProfil({
      ...value,
      competences: value.competences.split(',').map((item) => item.trim()).filter(Boolean),
    }).subscribe({
      next: () => { this.successMessage = 'Profil mis à jour.'; this.saving = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Le profil n'a pas pu être enregistré.";
        this.saving = false;
      },
    });
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.errorMessage = '';
    this.successMessage = '';
    this.selectedCv = null;
    this.cvInput = input;

    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.pdf') || (file.type && file.type !== 'application/pdf')) {
      this.errorMessage = 'Veuillez sélectionner un fichier PDF valide.';
      input.value = '';
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      this.errorMessage = 'Le CV ne doit pas dépasser 20 Mo.';
      input.value = '';
      return;
    }
    this.selectedCv = file;
  }

  uploadCv(): void {
    if (!this.selectedCv || this.uploadingCv) return;
    this.uploadingCv = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.uploaderCv(this.selectedCv).subscribe({
      next: (result) => {
        this.updateCvState(result.profil);
        this.applyExtraction(result.extraction);
        this.selectedCv = null;
        if (this.cvInput) this.cvInput.value = '';
        this.uploadingCv = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Le CV n'a pas pu être déposé.";
        this.uploadingCv = false;
      },
    });
  }

  extractCv(): void {
    if (this.extractingCv) return;
    this.extractingCv = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.extraireCv().subscribe({
      next: (extraction) => {
        this.applyExtraction(extraction);
        this.extractingCv = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Le CV n'a pas pu être analysé.";
        this.extractingCv = false;
      },
    });
  }

  downloadCv(): void {
    if (this.downloadingCv) return;
    this.downloadingCv = true;
    this.errorMessage = '';
    this.service.telechargerCv().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = this.cvNomFichier || 'cv.pdf';
        link.click();
        URL.revokeObjectURL(url);
        this.downloadingCv = false;
      },
      error: () => {
        this.errorMessage = "Le CV n'a pas pu être téléchargé.";
        this.downloadingCv = false;
      },
    });
  }

  viewCv(): void {
    if (this.previewingCv) return;
    this.previewingCv = true;
    this.errorMessage = '';
    this.service.telechargerCv().subscribe({
      next: (blob) => {
        this.releaseCvPreview();
        this.cvPreviewObjectUrl = URL.createObjectURL(blob);
        this.cvPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.cvPreviewObjectUrl);
        this.previewingCv = false;
      },
      error: () => {
        this.errorMessage = "Le CV n'a pas pu être affiché.";
        this.previewingCv = false;
      },
    });
  }

  closeCvPreview(): void {
    this.cvPreviewUrl = undefined;
    this.releaseCvPreview();
  }

  deleteCv(): void {
    if (this.deletingCv || !window.confirm('Supprimer définitivement votre CV ?')) return;
    this.deletingCv = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.supprimerCv().subscribe({
      next: () => {
        this.cvPresent = false;
        this.cvNomFichier = '';
        this.cvDateDepot = undefined;
        this.successMessage = 'Votre CV a été supprimé.';
        this.deletingCv = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Le CV n'a pas pu être supprimé.";
        this.deletingCv = false;
      },
    });
  }

  private updateCvState(profil: { cvPresent: boolean; cvNomFichier?: string; cvDateDepot?: string }): void {
    this.cvPresent = profil.cvPresent;
    this.cvNomFichier = profil.cvNomFichier ?? 'cv.pdf';
    this.cvDateDepot = profil.cvDateDepot;
  }

  private applyExtraction(extraction: CvExtraction): void {
    if (!extraction.texteDetecte) {
      this.successMessage = "Le CV est déposé, mais aucun texte exploitable n'a été détecté. Remplissez le profil manuellement.";
      return;
    }

    const values: Partial<{
      telephone: string;
      filiere: string;
      niveauEtudes: string;
      competences: string;
    }> = {};
    if (extraction.telephone) values.telephone = extraction.telephone;
    if (extraction.filiere) values.filiere = extraction.filiere;
    if (extraction.niveauEtudes) values.niveauEtudes = extraction.niveauEtudes;
    if (extraction.competences.length) values.competences = extraction.competences.join(', ');

    this.form.patchValue(values);
    this.form.markAsDirty();
    this.successMessage = Object.keys(values).length
      ? 'Les informations du CV ont prérempli le profil. Vérifiez-les puis cliquez sur « Enregistrer mon profil ».'
      : "Le texte du CV a été lu, mais aucune information de profil n'a été reconnue.";
  }

  private releaseCvPreview(): void {
    if (this.cvPreviewObjectUrl) {
      URL.revokeObjectURL(this.cvPreviewObjectUrl);
      this.cvPreviewObjectUrl = undefined;
    }
  }
}
