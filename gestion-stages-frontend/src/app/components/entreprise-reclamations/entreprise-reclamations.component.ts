import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Reclamation, StatutReclamation } from '../../models/reclamation.model';
import { AuthService } from '../../services/auth.service';
import { ReclamationService } from '../../services/reclamation.service';

@Component({
  selector: 'app-entreprise-reclamations',
  standalone: true,
  imports: [CommonModule, RouterLink],
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
            <h1>Réclamations des stagiaires</h1>
            <p>Suivez les demandes envoyées par vos étudiants et traitez-les rapidement.</p>
          </div>
        </div>

        <div *ngIf="errorMessage" class="alert alert-error">
          {{ errorMessage }}
        </div>

        <div *ngIf="isLoading" class="loading-state">
          Chargement des réclamations...
        </div>

        <div *ngIf="!isLoading && reclamations.length === 0" class="empty-state">
          Aucune réclamation n'a encore été envoyée à votre entreprise.
        </div>

        <div *ngIf="!isLoading && reclamations.length > 0" class="reclamations-grid">
          <article *ngFor="let reclamation of reclamations" class="reclamation-card">
            <div class="card-header">
              <div>
                <h2>{{ reclamation.objet }}</h2>
                <p>{{ reclamation.nomEtudiant }}</p>
              </div>
              <span class="status-badge" [ngClass]="'status-' + reclamation.statut">
                {{ getStatutLabel(reclamation.statut) }}
              </span>
            </div>

            <div class="card-body">
              <div class="info-row">
                <span class="info-label">Type</span>
                <span>{{ getTypeLabel(reclamation.typeReclamation) }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Créée le</span>
                <span>{{ reclamation.dateCreation | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <div class="info-row" *ngIf="reclamation.nomTraitePar">
                <span class="info-label">En charge</span>
                <span>{{ reclamation.nomTraitePar }}</span>
              </div>
            </div>

            <div class="card-footer">
              <a
                class="button primary"
                [routerLink]="['/espace-entreprise/reclamations', reclamation.id]"
              >
                Ouvrir et traiter
              </a>
            </div>
          </article>
        </div>
      </section>
    </main>
  `,
  styles: [`
    .portal-page {
      min-height: 100vh;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      padding: 20px;
    }

    .portal-shell {
      max-width: 1200px;
      margin: 0 auto;
    }

    .portal-nav {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32px;
      gap: 20px;
      flex-wrap: wrap;
    }

    .brand {
      font-size: 1.5rem;
      font-weight: 800;
      color: #1f2937;
      text-decoration: none;
      background: linear-gradient(90deg, #1f2937, #4b5563);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .portal-nav nav {
      display: flex;
      gap: 20px;
      align-items: center;
      flex-wrap: wrap;
    }

    .portal-nav a,
    .link-button {
      color: #4f46e5;
      text-decoration: none;
      font-weight: 600;
      background: transparent;
      border: none;
      cursor: pointer;
      font-size: 1rem;
    }

    .page-header {
      margin-bottom: 24px;
    }

    .eyebrow {
      margin: 0 0 8px;
      color: #6366f1;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      font-size: 0.8rem;
    }

    .page-header h1 {
      margin: 0 0 8px;
      color: #111827;
    }

    .page-header p {
      margin: 0;
      color: #6b7280;
    }

    .alert {
      padding: 16px 20px;
      border-radius: 12px;
      margin-bottom: 20px;
    }

    .alert-error {
      background: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }

    .loading-state,
    .empty-state {
      background: white;
      border-radius: 16px;
      padding: 48px 24px;
      text-align: center;
      color: #6b7280;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    }

    .reclamations-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 20px;
    }

    .reclamation-card {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      padding: 20px;
      border-bottom: 1px solid #e5e7eb;
      background: #f9fafb;
    }

    .card-header h2 {
      margin: 0 0 6px;
      color: #111827;
      font-size: 1.1rem;
    }

    .card-header p {
      margin: 0;
      color: #6b7280;
    }

    .status-badge {
      padding: 6px 12px;
      border-radius: 999px;
      font-size: 0.8rem;
      font-weight: 700;
      white-space: nowrap;
      height: fit-content;
    }

    .status-OUVERTE { background: #dbeafe; color: #1e40af; }
    .status-EN_TRAITEMENT { background: #fed7aa; color: #92400e; }
    .status-RESOLUE { background: #d1fae5; color: #065f46; }
    .status-CLOTUREE { background: #e5e7eb; color: #374151; }
    .status-REOUVERTE { background: #fee2e2; color: #991b1b; }

    .card-body {
      padding: 20px;
      display: grid;
      gap: 12px;
      flex: 1;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      color: #374151;
    }

    .info-label {
      font-weight: 700;
      color: #6b7280;
    }

    .card-footer {
      padding: 20px;
      border-top: 1px solid #e5e7eb;
      background: #f9fafb;
    }

    .button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      text-decoration: none;
      padding: 10px 16px;
      border-radius: 8px;
      font-weight: 700;
    }

    .button.primary {
      color: white;
      background: linear-gradient(135deg, #6366f1, #4f46e5);
    }
  `]
})
export class EntrepriseReclamationsComponent implements OnInit {
  private readonly reclamationService = inject(ReclamationService);
  private readonly authService = inject(AuthService);

  reclamations: Reclamation[] = [];
  isLoading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.reclamationService.listerReclamationsEntreprise().subscribe({
      next: (reclamations) => {
        this.reclamations = [...reclamations].sort(
          (a, b) => new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime()
        );
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message || 'Impossible de charger les réclamations.';
        this.isLoading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      NOTE: 'Note',
      DOCUMENT: 'Document',
      EVALUATION: 'Évaluation',
      AUTRE: 'Autre'
    };

    return labels[type] || type;
  }

  getStatutLabel(statut: StatutReclamation): string {
    const labels: Record<StatutReclamation, string> = {
      OUVERTE: 'Ouverte',
      EN_TRAITEMENT: 'En traitement',
      RESOLUE: 'Résolue',
      CLOTUREE: 'Clôturée',
      REOUVERTE: 'Réouverte'
    };

    return labels[statut];
  }
}
