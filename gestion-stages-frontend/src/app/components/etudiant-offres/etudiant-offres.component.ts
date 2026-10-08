import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, of, Subscription, switchMap, timer } from 'rxjs';

import { OffreStage } from '../../models/stage.model';
import { AuthService } from '../../services/auth.service';
import { EtudiantStageService } from '../../services/etudiant-stage.service';
import { RecommandationsComponent } from '../recommandations/recommandations.component';

@Component({
  selector: 'app-etudiant-offres',
  standalone: true,
  imports: [DatePipe, RouterLink, RecommandationsComponent],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav">
        <a class="brand" routerLink="/espace-etudiant">Gestion des stages</a>
        <nav><a routerLink="/espace-etudiant">Offres</a><a routerLink="/espace-etudiant/recommandations">Recommandations</a><a routerLink="/espace-etudiant/candidatures">Mes candidatures</a><a routerLink="/espace-etudiant/notifications">Notifications @if (unreadNotifications) { <span class="notification-count">{{ unreadNotifications }}</span> }</a><a routerLink="/espace-etudiant/reclamations">Réclamations</a><a routerLink="/espace-etudiant/profil">Mon profil</a><button class="link-button" type="button" (click)="logout()">Déconnexion</button></nav>
      </header>
      <div class="page-header"><div><p class="eyebrow">Espace étudiant</p><h1>Offres de stage</h1><p>{{ totalElements }} opportunité(s) disponible(s)</p></div></div>
      @if (unreadNotifications) {
        <div class="success notification-alert">
          Vous avez {{ unreadNotifications }} nouvelle(s) notification(s).
          <a routerLink="/espace-etudiant/notifications">Les consulter</a>
        </div>
      }
      @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
      @if (loading) { <div class="empty card">Chargement des offres…</div> }
      @else if (!offres.length) { <div class="empty card">Aucune offre disponible actuellement.</div> }
      @else {
        <div class="card-grid">
          @for (offre of offres; track offre.id) {
            <article class="offer-card">
              <div><span class="badge">{{ offre.typeStage || 'Stage' }}</span><h2>{{ offre.titre }}</h2><p class="company">{{ offre.entrepriseNom || 'Entreprise' }}</p></div>
              <p>{{ offre.description.length > 170 ? (offre.description.slice(0, 170) + '…') : offre.description }}</p>
              <div class="offer-meta"><span>📍 {{ offre.localisation }}</span><span>📅 {{ offre.dureeMois || '—' }} mois</span><span>Début {{ offre.dateDebut | date: 'dd/MM/yyyy' }}</span></div>
              <div class="tags">@for (skill of offre.competences.slice(0, 4); track skill) { <span>{{ skill }}</span> }</div>
              <a class="button primary" [routerLink]="['/espace-etudiant/offres', offre.id]">Voir l'offre</a>
            </article>
          }
        </div>
      }
      <footer class="pagination"><button [disabled]="page === 0" (click)="goToPage(page - 1)">Précédent</button><span>Page {{ totalPages ? page + 1 : 0 }} sur {{ totalPages }}</span><button [disabled]="page + 1 >= totalPages" (click)="goToPage(page + 1)">Suivant</button></footer>
    </section></main>
  `,
  styles: `
    .notification-count { display: inline-grid; place-items: center; min-width: 1.25rem; height: 1.25rem; padding: 0 .3rem; border-radius: 999px; color: #fff; background: #dc2626; font-size: .72rem; font-weight: 800; }
    .notification-alert { display: flex; justify-content: space-between; gap: 1rem; }
    .notification-alert a { color: inherit; font-weight: 800; }
  `,
})
export class EtudiantOffresComponent implements OnInit, OnDestroy {
  private readonly service = inject(EtudiantStageService);
  private readonly auth = inject(AuthService);
  offres: OffreStage[] = [];
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = true;
  errorMessage = '';
  unreadNotifications = 0;
  private notificationSubscription?: Subscription;

  ngOnInit(): void {
    this.load();
    this.notificationSubscription = timer(0, 30_000).pipe(
      switchMap(() => this.service.compterNotificationsNonLues().pipe(
        catchError(() => of({ nonLues: this.unreadNotifications })),
      )),
    ).subscribe({
      next: (result) => { this.unreadNotifications = result.nonLues; },
    });
  }
  ngOnDestroy(): void { this.notificationSubscription?.unsubscribe(); }
  load(): void {
    this.loading = true;
    this.service.listerOffres(this.page).subscribe({
      next: (result) => {
        this.offres = result.content; this.totalPages = result.totalPages;
        this.totalElements = result.totalElements; this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger les offres.';
        this.loading = false;
      },
    });
  }
  goToPage(page: number): void { this.page = page; this.load(); }
  logout(): void { this.auth.logout(); }
}
