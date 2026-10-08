import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Candidature, OffreStage, StatutCandidature } from '../../models/stage.model';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';

@Component({
  selector: 'app-offre-candidatures',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <a class="back-link" routerLink="/espace-entreprise">← Retour aux offres</a>
      <div class="page-header"><div><p class="eyebrow">Espace entreprise</p><h1>Candidatures reçues</h1><p>{{ candidatures.length }} candidature(s) · {{ offre?.nombrePlaces ?? '—' }} place(s) prévue(s)</p></div></div>
      @if (offre) {
        <section class="card capacity-summary">
          <div><strong>{{ offre.placesOccupees }} place(s) occupée(s) sur {{ offre.nombrePlaces }}</strong><p>{{ offre.placesRestantes }} place(s) restante(s)</p></div>
          <div class="capacity-bar" role="progressbar" aria-label="Places occupées" [attr.aria-valuenow]="occupationRate()" aria-valuemin="0" aria-valuemax="100">
            <span [style.width.%]="occupationRate()"></span>
          </div>
        </section>
      }
      @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
      @if (successMessage) { <div class="success">{{ successMessage }}</div> }
      <div class="table-wrapper"><table>
        <thead><tr><th>Candidat</th><th>Formation</th><th>Date</th><th>Statut</th><th>Actions</th></tr></thead>
        <tbody>
          @if (loading) { <tr><td colspan="5" class="empty">Chargement…</td></tr> }
          @else if (!candidatures.length) { <tr><td colspan="5" class="empty">Aucune candidature reçue.</td></tr> }
          @else {
            @for (item of candidatures; track item.id) {
              <tr>
                <td><strong>{{ item.etudiantPrenom }} {{ item.etudiantNom }}</strong><small>{{ item.etudiantEmail }}</small></td>
                <td>{{ item.filiere || '—' }}<small>{{ item.niveauEtudes || 'Niveau non renseigné' }}</small></td>
                <td>{{ item.dateCandidature | date: 'dd/MM/yyyy à HH:mm' }}</td>
                <td><span class="badge">{{ item.statut }}</span></td>
                <td class="row-actions">
                  <button type="button" class="accept-button" (click)="updateStatus(item, 'ACCEPTEE')" [disabled]="updatingId !== null || item.statut === 'ACCEPTEE' || placesCompletes">Accepter</button>
                  <button type="button" class="reject-button" (click)="updateStatus(item, 'REFUSEE')" [disabled]="updatingId !== null || item.statut === 'REFUSEE'">Refuser</button>
                  <a [routerLink]="['/espace-entreprise/candidatures', item.id]">Voir le profil</a>
                  @if (item.statut === 'ACCEPTEE') {
                    <a [routerLink]="['/espace-entreprise/stages', item.id, 'taches-assignees', 'assigner']">Tâches du stagiaire</a>
                  }
                </td>
              </tr>
            }
          }
        </tbody>
      </table></div>
    </section></main>
  `,
  styles: `
    .row-actions { min-width: 25rem; }
    .row-actions > * { margin-right: .6rem; }
    .accept-button, .reject-button { min-height: 2.1rem; padding: 0 .7rem; font-weight: 700; }
    .accept-button { border: 0; color: #fff; background: #15803d; }
    .reject-button { border: 1px solid #dc2626; color: #b91c1c; background: #fff; }
    .accept-button:disabled, .reject-button:disabled { opacity: .5; cursor: not-allowed; }
    .capacity-summary { display: grid; grid-template-columns: minmax(15rem, 1fr) 2fr; align-items: center; gap: 1.5rem; margin-bottom: 1rem; }
    .capacity-summary p { margin: .25rem 0 0; color: #64748b; }
    .capacity-bar { width: 100%; height: .8rem; overflow: hidden; border-radius: 999px; background: #e2e8f0; }
    .capacity-bar span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2563eb, #16a34a); transition: width .3s ease; }
    @media (max-width: 40rem) { .capacity-summary { grid-template-columns: 1fr; gap: .75rem; } }
  `,
})
export class OffreCandidaturesComponent implements OnInit {
  private readonly service = inject(EntrepriseStageService);
  private readonly offreId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  offre: OffreStage | null = null;
  candidatures: Candidature[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';
  updatingId: number | null = null;

  get placesCompletes(): boolean {
    return !!this.offre && this.offre.placesRestantes <= 0;
  }

  occupationRate(): number {
    if (!this.offre?.nombrePlaces) return 0;
    return Math.min(100, Math.round((this.offre.placesOccupees / this.offre.nombrePlaces) * 100));
  }

  ngOnInit(): void {
    forkJoin({
      offre: this.service.obtenirOffre(this.offreId),
      candidatures: this.service.listerCandidatures(this.offreId),
    }).subscribe({
      next: ({ offre, candidatures }) => {
        this.offre = offre;
        this.candidatures = candidatures.content;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger les candidatures.';
        this.loading = false;
      },
    });
  }

  updateStatus(
    candidature: Candidature,
    statut: Exclude<StatutCandidature, 'EN_ATTENTE'>,
  ): void {
    if (this.updatingId !== null) return;
    this.updatingId = candidature.id;
    this.errorMessage = '';
    this.successMessage = '';
    this.service.modifierStatutCandidature(candidature.id, statut).subscribe({
      next: (updated) => {
        if (this.offre && candidature.statut !== updated.statut) {
          const difference = updated.statut === 'ACCEPTEE' ? 1
            : candidature.statut === 'ACCEPTEE' ? -1 : 0;
          this.offre.placesOccupees += difference;
          this.offre.placesRestantes = Math.max(
            0,
            this.offre.nombrePlaces - this.offre.placesOccupees,
          );
        }
        this.candidatures = this.candidatures.map((item) =>
          item.id === updated.id ? updated : item
        );
        this.successMessage = statut === 'ACCEPTEE'
          ? 'La candidature a été acceptée.'
          : 'La candidature a été refusée.';
        this.updatingId = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "La décision n'a pas pu être enregistrée.";
        this.updatingId = null;
      },
    });
  }
}
