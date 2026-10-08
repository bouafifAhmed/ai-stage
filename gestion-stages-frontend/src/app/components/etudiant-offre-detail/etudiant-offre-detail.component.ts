import { DatePipe } from '@angular/common';

import { HttpErrorResponse } from '@angular/common/http';

import { Component, inject, OnInit } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { forkJoin, switchMap } from 'rxjs';



import { QcmStatut } from '../../models/qcm.model';
import { OffreStage } from '../../models/stage.model';
import { EtudiantStageService } from '../../services/etudiant-stage.service';
import { QcmService } from '../../services/qcm.service';
import { QcmPassageComponent } from '../qcm-passage/qcm-passage.component';
import { RecommandationService } from '../../services/recommandation.service';
import { AdequationAnalyse } from '../../models/recommandation.model';




type EtapeCandidature = 'formulaire' | 'qcm' | 'bloque' | 'succes';



@Component({

  selector: 'app-etudiant-offre-detail',

  standalone: true,

  imports: [DatePipe, ReactiveFormsModule, RouterLink, QcmPassageComponent],

  template: `

    <main class="portal-page"><section class="portal-shell">

      <a class="back-link" routerLink="/espace-etudiant">← Retour au catalogue</a>

      @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }

      @if (loading) { <div class="card">Chargement…</div> }

      @else if (offre) {

        <div class="detail-layout" [class.detail-layout--qcm]="etape === 'qcm'">

          <article class="card">

            <p class="eyebrow">{{ offre.entrepriseNom || 'Offre de stage' }}</p>

            <h1>{{ offre.titre }}</h1>

            <div class="offer-meta"><span>📍 {{ offre.localisation }}</span><span>📅 {{ offre.dureeMois || '—' }} mois</span><span>Début {{ offre.dateDebut | date: 'dd/MM/yyyy' }}</span></div>

            <div class="capacity-summary">

              <div><strong>{{ offre.placesRestantes }} place(s) restante(s)</strong><span>{{ offre.placesOccupees }} / {{ offre.nombrePlaces }} occupée(s)</span></div>

              <div class="capacity-bar" role="progressbar" aria-label="Places occupées" [attr.aria-valuenow]="occupationRate()" aria-valuemin="0" aria-valuemax="100">

                <span [style.width.%]="occupationRate()"></span>

              </div>

            </div>

            <h2>Description</h2><p class="pre-line">{{ offre.description }}</p>

            <h2>Profil recherché</h2>

            <p>{{ offre.niveauRequis || 'Niveau non précisé' }}</p>

            <div class="tags">@for (skill of offre.competences; track skill) { <span>{{ skill }}</span> }</div>

            @if (adequation) {
              <div class="ai-fit-card">
                <div class="ai-fit-header">
                  <div class="ai-title-wrap">
                    <span class="ai-badge">🤖 Analyse IA</span>
                    <h3 class="ai-fit-title">Compatibilité avec votre profil</h3>
                  </div>
                  <span class="ai-score-pill" [class.high]="adequation.scorePourcentage >= 70" [class.medium]="adequation.scorePourcentage >= 40 && adequation.scorePourcentage < 70" [class.low]="adequation.scorePourcentage < 40">
                    {{ adequation.scorePourcentage }}% de match
                  </span>
                </div>

                <div class="ai-bar-track">
                  <div class="ai-bar-fill" [style.width.%]="adequation.scorePourcentage" [class.high]="adequation.scorePourcentage >= 70" [class.medium]="adequation.scorePourcentage >= 40 && adequation.scorePourcentage < 70" [class.low]="adequation.scorePourcentage < 40"></div>
                </div>

                <div class="ai-skills-comparison">
                  @if (adequation.competencesAcquises.length) {
                    <div class="skill-col">
                      <p class="skill-col-label label-success">✓ Vos compétences en adéquation :</p>
                      <div class="tags">
                        @for (s of adequation.competencesAcquises; track s) {
                          <span class="tag-match">{{ s }}</span>
                        }
                      </div>
                    </div>
                  }
                  @if (adequation.competencesManquantes.length) {
                    <div class="skill-col">
                      <p class="skill-col-label label-missing">⚡ Compétences complémentaires :</p>
                      <div class="tags">
                        @for (s of adequation.competencesManquantes; track s) {
                          <span class="tag-missing">{{ s }}</span>
                        }
                      </div>
                    </div>
                  }
                </div>

                @if (adequation.conseils.length) {
                  <div class="ai-advice-box">
                    <p class="advice-heading">💡 Conseils personnalisés IA pour candidater :</p>
                    <ul>
                      @for (c of adequation.conseils; track c) {
                        <li>{{ c }}</li>
                      }
                    </ul>
                  </div>
                }
              </div>
            }

            @if (offre.remuneration) { <h2>Rémunération</h2><p>{{ offre.remuneration }} par mois</p> }

            <p class="muted">Candidatures avant le {{ offre.dateLimite | date: 'dd/MM/yyyy' }}</p>

          </article>

          <aside class="card" [class.qcm-aside]="etape === 'qcm'">

            <h2>@if (etape !== 'qcm') { Postuler }</h2>

            @if (etape === 'succes') {

              <div class="success">Votre candidature a bien été envoyée.</div>

              <a class="button secondary" routerLink="/espace-etudiant/candidatures">Voir mes candidatures</a>

            } @else if (etape === 'bloque') {

              <div class="alert">

                <p><strong>QCM d'admissibilité non validé</strong></p>

                <p>

                  Vous avez utilisé toutes vos tentatives

                  @if (qcmStatut?.meilleurScore !== null && qcmStatut?.meilleurScore !== undefined) {

                    (meilleur score : {{ qcmStatut?.meilleurScore }} %).

                  } @else {

                    .

                  }

                </p>

                <p>Contactez l'administration pour obtenir de l'aide.</p>

              </div>

            } @else if (etape === 'qcm') {
              <app-qcm-passage (passed)="onQcmReussi()" />

            } @else {

              <p>Vérifiez votre profil avant d'envoyer votre candidature.</p>

              @if (qcmStatut?.reussi) {

                <div class="success-inline">QCM d'admissibilité validé.</div>

              }

              <form [formGroup]="form" (ngSubmit)="submit()">

                <label>Téléphone *<input formControlName="telephone" /></label>

                <label>Filière *<input formControlName="filiere" /></label>

                <label>Niveau *<input formControlName="niveauEtudes" /></label>

                <label>Compétences *<input formControlName="competences" placeholder="Angular, Java, SQL" /><small>Séparez par des virgules.</small></label>

                <label>Message de motivation *<textarea rows="7" formControlName="message" placeholder="Expliquez votre intérêt pour ce stage…"></textarea></label>

                @if (form.invalid && form.touched) { <p class="field-error">Tous les champs sont obligatoires (message de 20 caractères minimum).</p> }

                <button class="primary full-button" type="submit" [disabled]="submitting">{{ submitting ? 'Envoi…' : 'Envoyer ma candidature' }}</button>

              </form>

            }

          </aside>

        </div>

      }

    </section></main>

  `,

  styles: `

    .capacity-summary { margin: 1.25rem 0; padding: 1rem; border-radius: .75rem; background: #f8fafc; }

    .capacity-summary > div:first-child { display: flex; justify-content: space-between; gap: 1rem; margin-bottom: .6rem; }

    .capacity-summary span { color: #64748b; }

    .capacity-bar { width: 100%; height: .7rem; overflow: hidden; border-radius: 999px; background: #e2e8f0; }

    .capacity-bar span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2563eb, #16a34a); }

    .success-inline { margin-bottom: 1rem; padding: .75rem; border-radius: .5rem; color: #166534; background: #dcfce7; }
    .detail-layout--qcm { grid-template-columns: 1fr; }
    .detail-layout--qcm article { display: none; }
    .qcm-aside {
      padding: 0;
      border: 0;
      background: transparent;
      box-shadow: none;
      position: static;
    }

    .ai-fit-card { margin: 1.5rem 0; padding: 1.25rem; border-radius: .75rem; background: #f8fafc; border: 1px solid #e2e8f0; }
    .ai-fit-header { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: .75rem; }
    .ai-title-wrap { display: flex; align-items: center; gap: .5rem; }
    .ai-badge { display: inline-block; padding: .2rem .5rem; border-radius: .35rem; font-size: .75rem; font-weight: 700; background: #e0e7ff; color: #4338ca; }
    .ai-fit-title { margin: 0; font-size: 1.05rem; font-weight: 700; color: #1e293b; }
    .ai-score-pill { padding: .25rem .65rem; border-radius: 999px; font-size: .85rem; font-weight: 800; }
    .ai-score-pill.high { background: #dcfce7; color: #166534; }
    .ai-score-pill.medium { background: #fef3c7; color: #92400e; }
    .ai-score-pill.low { background: #fee2e2; color: #991b1b; }
    .ai-bar-track { width: 100%; height: .6rem; border-radius: 999px; background: #e2e8f0; overflow: hidden; margin-bottom: 1rem; }
    .ai-bar-fill { height: 100%; border-radius: inherit; transition: width .4s ease; }
    .ai-bar-fill.high { background: #16a34a; }
    .ai-bar-fill.medium { background: #d97706; }
    .ai-bar-fill.low { background: #dc2626; }
    .ai-skills-comparison { display: flex; flex-direction: column; gap: .75rem; margin-bottom: 1rem; }
    .skill-col-label { margin: 0 0 .35rem; font-size: .85rem; font-weight: 700; }
    .label-success { color: #166534; }
    .label-missing { color: #b45309; }
    .tag-match { background: #dcfce7 !important; color: #166534 !important; border: 1px solid #bbf7d0; }
    .tag-missing { background: #fef3c7 !important; color: #92400e !important; border: 1px solid #fde68a; }
    .ai-advice-box { padding: .75rem 1rem; border-radius: .5rem; background: #f0fdf4; border-left: 4px solid #22c55e; }
    .advice-heading { margin: 0 0 .35rem; font-size: .85rem; font-weight: 700; color: #15803d; }
    .ai-advice-box ul { margin: 0; padding-left: 1.25rem; font-size: .85rem; color: #166534; }
    .ai-advice-box li { margin-bottom: .25rem; }

  `,

})

export class EtudiantOffreDetailComponent implements OnInit {

  private readonly service = inject(EtudiantStageService);

  private readonly qcmService = inject(QcmService);

  private readonly recommandationService = inject(RecommandationService);

  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  private readonly router = inject(Router);

  private readonly fb = inject(FormBuilder);

  offre: OffreStage | null = null;

  adequation: AdequationAnalyse | null = null;

  qcmStatut: QcmStatut | null = null;

  etape: EtapeCandidature = 'formulaire';

  loading = true;

  submitting = false;

  errorMessage = '';



  readonly form = this.fb.nonNullable.group({

    telephone: ['', Validators.required],

    filiere: ['', Validators.required],

    niveauEtudes: ['', Validators.required],

    competences: ['', Validators.required],

    message: ['', [Validators.required, Validators.minLength(20)]],

  });



  ngOnInit(): void {

    forkJoin({

      offre: this.service.obtenirOffre(this.id),

      profil: this.service.obtenirProfil(),

      qcmStatut: this.qcmService.obtenirStatut(),

    }).subscribe({

      next: ({ offre, profil, qcmStatut }) => {

        this.offre = offre;

        this.qcmStatut = qcmStatut;

        this.etape = this.resoudreEtape(qcmStatut);

        this.form.patchValue({

          telephone: profil.telephone,

          filiere: profil.filiere,

          niveauEtudes: profil.niveauEtudes,

          competences: profil.competences.join(', '),

        });

        this.loading = false;

        this.recommandationService.obtenirAdequation(this.id).subscribe({
          next: (res) => {
            this.adequation = res;
          },
          error: () => {
            this.adequation = null;
          }
        });

      },

      error: (error: HttpErrorResponse) => {

        this.errorMessage = error.error?.message ?? "Impossible de charger l'offre et votre profil.";

        this.loading = false;

      },

    });

  }



  occupationRate(): number {

    if (!this.offre?.nombrePlaces) return 0;

    return Math.min(100, Math.round((this.offre.placesOccupees / this.offre.nombrePlaces) * 100));

  }



  onQcmReussi(): void {

    this.qcmService.obtenirStatut().subscribe({

      next: (statut) => {

        this.qcmStatut = statut;

        this.etape = 'formulaire';

      },

      error: (error: HttpErrorResponse) => {

        this.errorMessage = error.error?.message ?? 'Impossible de vérifier votre statut QCM.';

      },

    });

  }



  submit(): void {

    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.submitting = true;

    this.errorMessage = '';

    const value = this.form.getRawValue();

    this.service.modifierProfil({

      telephone: value.telephone,

      filiere: value.filiere,

      niveauEtudes: value.niveauEtudes,

      competences: value.competences.split(',').map((item) => item.trim()).filter(Boolean),

    }).pipe(

      switchMap(() => this.service.postuler({ offreId: this.id, message: value.message })),

    ).subscribe({

      next: () => { this.etape = 'succes'; this.submitting = false; },

      error: (error: HttpErrorResponse) => {

        this.errorMessage = error.error?.message ?? "La candidature n'a pas pu être envoyée.";

        this.submitting = false;

        if (error.status === 409 && this.errorMessage.toLowerCase().includes('qcm')) {

          this.etape = 'qcm';

        }

        window.scrollTo({ top: 0, behavior: 'smooth' });

      },

    });

  }



  cancel(): void { void this.router.navigate(['/espace-etudiant']); }



  private resoudreEtape(statut: QcmStatut): EtapeCandidature {

    if (statut.admissible) {

      return 'formulaire';

    }

    if (statut.tentativesRestantes > 0) {

      return 'qcm';

    }

    return 'bloque';

  }

}


