import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { ModeTravail, OffreStage } from '../../models/stage.model';
import { AuthService } from '../../services/auth.service';
import { EntrepriseStageService } from '../../services/entreprise-stage.service';

@Component({
  selector: 'app-entreprise-offres',
  standalone: true,
  imports: [DatePipe, RouterLink, RouterLinkActive],
  template: `
    <main class="portal-page">
      <section class="portal-shell">
        <header class="portal-nav">
          <a class="brand" routerLink="/espace-entreprise">Gestion des stages</a>
          <nav>
            <a routerLink="/espace-entreprise" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Mes offres</a>
            <a routerLink="/espace-entreprise/reclamations" routerLinkActive="active">Réclamations</a>
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
        @if (loading) {
          <div class="empty card">Chargement…</div>
        } @else if (!offres.length) {
          <div class="empty card empty-offer">
            <strong>Aucune offre. Créez votre première offre de stage.</strong>
            <a class="button primary" routerLink="/espace-entreprise/offres/nouvelle">Créer une offre</a>
          </div>
        } @else {
          <div class="offers-board">
            @for (offre of offres; track offre.id) {
              <article class="job-card" [attr.data-statut]="offre.statut || 'PUBLIEE'">
                <div class="job-top">
                  <div>
                    <p class="job-kicker">{{ offre.domaine || 'Stage' }} · {{ offre.typeStage || 'Offre' }}</p>
                    <h2>{{ offre.titre }}</h2>
                  </div>
                  <span class="badge {{ statutBadge(offre) }}">{{ statutLabel(offre) }}</span>
                </div>
                @if (offre.description) {
                  <p class="job-summary">{{ resume(offre.description) }}</p>
                }
                <ul class="job-facts">
                  <li><span>Lieu</span><strong>{{ offre.localisation || '—' }}</strong></li>
                  <li><span>Durée</span><strong>{{ offre.dureeMois || '—' }} mois</strong></li>
                  <li><span>Mode</span><strong>{{ modeLabel(offre.mode) }}</strong></li>
                  <li><span>Début</span><strong>{{ offre.dateDebut ? (offre.dateDebut | date: 'dd/MM/yyyy') : '—' }}</strong></li>
                  <li><span>Limite</span><strong>{{ offre.dateLimite | date: 'dd/MM/yyyy' }}</strong></li>
                </ul>
                <div class="job-capacity">
                  <div>
                    <strong>{{ offre.placesOccupees }} / {{ offre.nombrePlaces }}</strong>
                    <small>{{ offre.placesRestantes }} place(s) restante(s)</small>
                  </div>
                  <div class="capacity-bar" role="progressbar" aria-label="Places occupées" [attr.aria-valuenow]="occupationRate(offre)" aria-valuemin="0" aria-valuemax="100">
                    <span [style.width.%]="occupationRate(offre)"></span>
                  </div>
                </div>
                @if (offre.competences.length) {
                  <div class="tags">
                    @for (skill of offre.competences.slice(0, 4); track skill) { <span>{{ skill }}</span> }
                  </div>
                }
                <div class="job-actions">
                  <a class="button secondary" [routerLink]="['/espace-entreprise/offres', offre.id, 'candidatures']">Candidatures</a>
                  <a class="button secondary" [routerLink]="['/espace-entreprise/offres', offre.id, 'modifier']">Modifier</a>
                  @if (pendingDeleteId === offre.id) {
                    <span class="confirm-delete">
                      Supprimer cette offre ?
                      <button class="danger-text link-button" type="button" (click)="delete(offre.id)">Confirmer</button>
                      <button class="link-button" type="button" (click)="pendingDeleteId = null">Annuler</button>
                    </span>
                  } @else {
                    <button class="danger-text link-button" type="button" (click)="pendingDeleteId = offre.id">Supprimer</button>
                  }
                </div>
              </article>
            }
          </div>
        }
        <footer class="pagination">
          <button type="button" [disabled]="page === 0" (click)="goToPage(page - 1)">Précédent</button>
          <span>Page {{ totalPages ? page + 1 : 0 }} sur {{ totalPages }}</span>
          <button type="button" [disabled]="page + 1 >= totalPages" (click)="goToPage(page + 1)">Suivant</button>
        </footer>
      </section>
    </main>
  `,
  styles: `
    .page-header .primary { align-self: flex-start; width: auto; }
    .offers-board { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1.15rem; }
    .job-card { display: flex; flex-direction: column; gap: .9rem; padding: 1.25rem 1.3rem 1.1rem; border: 1px solid #e2e8f0; border-radius: 1.1rem; background: #fff; box-shadow: 0 10px 28px -18px rgba(15, 23, 42, .45); border-top: 4px solid #16a34a; }
    .job-card[data-statut="BROUILLON"] { border-top-color: #d97706; }
    .job-card[data-statut="CLOTUREE"] { border-top-color: #64748b; }
    .job-card[data-statut="ARCHIVEE"] { border-top-color: #94a3b8; }
    .job-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
    .job-kicker { margin: 0 0 .25rem; color: #64748b; font-size: .78rem; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; }
    .job-card h2 { margin: 0; font-size: 1.2rem; line-height: 1.3; letter-spacing: -.02em; }
    .job-summary { margin: 0; color: #475569; font-size: .92rem; line-height: 1.5; }
    .job-facts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .65rem; margin: 0; padding: 0; list-style: none; }
    .job-facts li { padding: .65rem .75rem; border-radius: .75rem; background: #f8fafc; }
    .job-facts li:last-child:nth-child(odd) { grid-column: 1 / -1; }
    .job-facts span { display: block; color: #64748b; font-size: .72rem; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; }
    .job-facts strong { display: block; margin-top: .15rem; font-size: .9rem; }
    .job-capacity { display: grid; gap: .4rem; }
    .job-capacity > div:first-child { display: flex; justify-content: space-between; gap: .75rem; align-items: baseline; }
    .job-capacity small { color: #64748b; }
    .capacity-bar { width: 100%; height: .55rem; overflow: hidden; border-radius: 999px; background: #e2e8f0; }
    .capacity-bar span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #2563eb, #16a34a); }
    .job-actions { display: flex; flex-wrap: wrap; align-items: center; gap: .55rem; margin-top: auto; padding-top: .85rem; border-top: 1px solid #f1f5f9; }
    .job-actions .button { min-height: 2.35rem; padding: 0 .9rem; }
    .confirm-delete { display: inline-flex; flex-wrap: wrap; align-items: center; gap: .15rem; color: #991b1b; font-size: .85rem; font-weight: 700; }
    .status-cloturee { color: #334155; background: #e2e8f0; border-color: #cbd5e1; }
    .status-archivee { color: #475569; background: #f8fafc; border-color: #e2e8f0; }
    .empty-offer { display: grid; justify-items: center; gap: 1rem; }
    .empty-offer strong { color: #0f172a; font-size: 1rem; }
    @media (max-width: 64rem) { .offers-board { grid-template-columns: 1fr; } }
    @media (max-width: 32rem) { .job-facts { grid-template-columns: 1fr; } }
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

  statutLabel(offre: OffreStage): string {
    switch (offre.statut) {
      case 'BROUILLON': return 'Brouillon';
      case 'CLOTUREE': return 'Clôturée';
      case 'ARCHIVEE': return 'Archivée';
      default: return 'Publiée';
    }
  }

  statutBadge(offre: OffreStage): string {
    switch (offre.statut) {
      case 'BROUILLON': return 'status-brouillon';
      case 'CLOTUREE': return 'status-cloturee';
      case 'ARCHIVEE': return 'status-archivee';
      default: return 'status-validee';
    }
  }

  modeLabel(mode: ModeTravail): string {
    switch (mode) {
      case 'HYBRIDE': return 'Hybride';
      case 'DISTANCIEL': return 'Distanciel';
      default: return 'Présentiel';
    }
  }

  resume(description: string): string {
    const text = description.trim();
    return text.length <= 150 ? text : `${text.slice(0, 150).trimEnd()}…`;
  }

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
