import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationEtudiant, TypeNotification } from '../../models/stage.model';
import { EtudiantStageService } from '../../services/etudiant-stage.service';

@Component({
  selector: 'app-notifications-etudiant',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell narrow">
      <header class="portal-nav">
        <a class="brand" routerLink="/espace-etudiant">Gestion des stages</a>
        <nav>
          <a routerLink="/espace-etudiant">Offres</a>
          <a routerLink="/espace-etudiant/candidatures">Mes candidatures</a>
          <a routerLink="/espace-etudiant/profil">Mon profil</a>
        </nav>
      </header>

      <div class="page-header">
        <div>
          <p class="eyebrow">Espace étudiant</p>
          <h1>Mes notifications</h1>
          <p>Retrouvez ici les réponses à vos candidatures et les nouvelles tâches de stage.</p>
        </div>
      </div>

      @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
      @if (loading) {
        <div class="card empty">Chargement des notifications…</div>
      } @else if (!notifications.length) {
        <div class="card empty">Vous n'avez aucune notification pour le moment.</div>
      } @else {
        <div class="notification-list">
          @for (notification of notifications; track notification.id) {
            <article class="card notification" [class.unread]="!notification.lue">
              <div>
                @if (!notification.lue) { <span class="badge">Nouvelle</span> }
                <h2>{{ title(notification.type) }}</h2>
                <p>{{ notification.message }}</p>
                <small>{{ notification.dateCreation | date:'dd/MM/yyyy à HH:mm' }}</small>
              </div>
              <div class="notification-actions">
                <a class="button primary" [routerLink]="link(notification)">{{ linkLabel(notification.type) }}</a>
                @if (!notification.lue) {
                  <button type="button" class="link-button" (click)="markAsRead(notification)" [disabled]="updatingId === notification.id">
                    {{ updatingId === notification.id ? 'Mise à jour…' : 'Marquer comme lue' }}
                  </button>
                }
              </div>
            </article>
          }
        </div>
      }
    </section></main>
  `,
  styles: `
    .notification-list { display: grid; gap: 1rem; }
    .notification { display: flex; align-items: center; justify-content: space-between; gap: 1.5rem; }
    .notification.unread { border-left: .3rem solid #15803d; background: #f0fdf4; }
    .notification h2 { margin: .6rem 0 .3rem; }
    .notification p { margin: 0; line-height: 1.5; }
    .notification small { display: block; margin-top: .7rem; color: #64748b; }
    .notification-actions { display: flex; align-items: center; flex-direction: column; gap: .75rem; min-width: 12rem; }
    @media (max-width: 40rem) { .notification { align-items: stretch; flex-direction: column; } }
  `,
})
export class NotificationsEtudiantComponent implements OnInit {
  private readonly service = inject(EtudiantStageService);
  notifications: NotificationEtudiant[] = [];
  loading = true;
  errorMessage = '';
  updatingId: number | null = null;

  ngOnInit(): void {
    this.service.listerNotifications().subscribe({
      next: (result) => {
        this.notifications = result.content;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger les notifications.';
        this.loading = false;
      },
    });
  }

  title(type: TypeNotification): string {
    return type === 'TACHE_ASSIGNEE' ? 'Nouvelle tâche assignée' : 'Candidature acceptée';
  }

  linkLabel(type: TypeNotification): string {
    return type === 'TACHE_ASSIGNEE' ? 'Voir mes tâches' : 'Voir ma candidature';
  }

  link(notification: NotificationEtudiant): string[] {
    if (notification.type === 'TACHE_ASSIGNEE') {
      return ['/espace-etudiant/stages', String(notification.candidatureId), 'taches-assignees'];
    }
    return ['/espace-etudiant/candidatures'];
  }

  markAsRead(notification: NotificationEtudiant): void {
    if (this.updatingId !== null) return;
    this.updatingId = notification.id;
    this.service.marquerNotificationLue(notification.id).subscribe({
      next: (updated) => {
        this.notifications = this.notifications.map((item) =>
          item.id === updated.id ? updated : item
        );
        this.updatingId = null;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? "La notification n'a pas pu être mise à jour.";
        this.updatingId = null;
      },
    });
  }
}
