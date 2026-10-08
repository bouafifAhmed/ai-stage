import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { OffreStage } from '../../models/stage.model';
import { AuthService } from '../../services/auth.service';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';

@Component({
  selector: 'app-entreprise-offres',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <main class="portal-page">
      <section class="portal-shell">
        <header class="portal-nav">
          <a class="brand" routerLink="/espace-entreprise">Gestion des stages</a>
          <nav>
            <a routerLink="/espace-entreprise">Mes offres</a>
            <a routerLink="/espace-entreprise/reclamations">Réclamations</a>
            <button class="link-button" type="button" (click)="logout()">Déconnexion</button>
          </nav>
        </header>
        <div class="page-header">
          <div>
            <p class="eyebrow">Espace entreprise</p>
            <h1>Mes offres de stage</h1>
            <p>{{ totalElements }} offre(s) publiée(s)</p>
          </div>
          <a class="button primary" routerLink="/espace-entreprise/offres/nouvelle">
            Créer une offre
          </a>
        </div>
        @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
        <div class="table-wrapper">
          <table>
            <thead><tr><th>Offre</th><th>Dates</th><th>Places</th><th>Statut</th><th>Actions</th></tr></thead>
            <tbody>
              @if (loading) {
                <tr><td colspan="5" class="empty">Chargement…</td></tr>
              } @else if (!offres.length) {
                <tr><td colspan="5" class="empty">Aucune offre. Créez votre première offre de stage.</td></tr>
              } @else {
                @for (offre of offres; track offre.id) {
                  <tr>
                    <td><strong>{{ offre.titre }}</strong><small>{{ offre.localisation }} · {{ offre.dureeMois || '—' }} mois</small></td>
                    <td>{{ offre.dateDebut | date: 'dd/MM/yyyy' }}<small>Limite : {{ offre.dateLimite | date: 'dd/MM/yyyy' }}</small></td>
                    <td class="capacity-cell">
                      <strong>{{ offre.placesOccupees }} / {{ offre.nombrePlaces }}</strong>
                      <small>{{ offre.placesRestantes }} place(s) restante(s)</small>
                      <div class="capacity-bar" role="progressbar" aria-label="Places occupées" [attr.aria-valuenow]="occupationRate(offre)" aria-valuemin="0" aria-valuemax="100">
                        <span [style.width.%]="occupationRate(offre)"></span>
                      </div>
                    </td>
                    <td><span class="badge">{{ offre.statut || 'PUBLIEE' }}</span></td>
                    <td class="actions">
                      <a [routerLink]="['/espace-entreprise/offres', offre.id, 'candidatures']">Candidatures</a>
                      <a [routerLink]="['/espace-entreprise/offres', offre.id, 'modifier']">Modifier</a>
                      @if (pendingDeleteId === offre.id) {
                        <button class="danger-text link-button" type="button" (click)="delete(offre.id)">Confirmer</button>
                        <button class="link-button" type="button" (click)="pendingDeleteId = null">Annuler</button>
                      } @else {
                        <button class="danger-text link-button" type="button" (click)="pendingDeleteId = offre.id">Supprimer</button>
                      }
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
        <footer class="pagination">
          <button type="button" [disabled]="page === 0" (click)="goToPage(page - 1)">Précédent</button>
          <span>Page {{ totalPages ? page + 1 : 0 }} sur {{ totalPages }}</span>
          <button type="button" [disabled]="page + 1 >= totalPages" (click)="goToPage(page + 1)">Suivant</button>
        </footer>
      </section>
    </main>
  `,
  styles: `
    .capacity-cell { min-width: 11rem; }
    .capacity-bar { width: 100%; height: .55rem; margin-top: .45rem; overflow: hidden; border-radius: 999px; background: #e2e8f0; }
    .capacity-bar span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2563eb, #16a34a); transition: width .3s ease; }
  `,
})
export class EntrepriseOffresComponent implements OnInit {
  private readonly service = inject(EntrepriseStageService);
  private readonly auth = inject(AuthService);
  offres: OffreStage[] = [];
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  errorMessage = '';
  pendingDeleteId: number | null = null;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.listerOffres(this.page).subscribe({
      next: (result) => {
        this.offres = result.content;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger les offres.';
        this.loading = false;
      },
    });
  }

  goToPage(page: number): void { this.page = page; this.load(); }

  occupationRate(offre: OffreStage): number {
    if (!offre.nombrePlaces) return 0;
    return Math.min(100, Math.round((offre.placesOccupees / offre.nombrePlaces) * 100));
  }

  delete(id: number): void {
    this.service.supprimerOffre(id).subscribe({
      next: () => { this.pendingDeleteId = null; this.load(); },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "Impossible de supprimer l'offre.";
        this.pendingDeleteId = null;
      },
    });
  }

  logout(): void { this.auth.logout(); }
}
