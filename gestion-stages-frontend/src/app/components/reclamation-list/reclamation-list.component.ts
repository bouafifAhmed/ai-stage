import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ReclamationService } from '../../services/reclamation.service';
import { EtudiantStageService } from '../../services/etudiant-stage.service';
import { Reclamation, StatutReclamation } from '../../models/reclamation.model';
import { ReclamationFormComponent } from '../reclamation-form/reclamation-form.component';

@Component({
  selector: 'app-reclamation-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReclamationFormComponent],
  template: `
    <main class="portal-page">
      <section class="portal-shell">
        <header class="portal-nav">
          <a class="brand" routerLink="/espace-etudiant">Gestion des stages</a>
          <nav>
            <a routerLink="/espace-etudiant">Offres</a>
            <a routerLink="/espace-etudiant/candidatures">Mes candidatures</a>
            <a routerLink="/espace-etudiant/notifications">Notifications</a>
            <a routerLink="/espace-etudiant/profil">Mon profil</a>
          </nav>
        </header>

        <div class="reclamations-container">
          <header class="reclamations-header">
            <div>
              <h1 class="page-title">Mes Réclamations</h1>
              <p class="page-subtitle">Consultez et suivez vos réclamations en cours</p>
            </div>
          </header>

          <div *ngIf="errorMessage" class="alert alert-error">
            {{ errorMessage }}
          </div>

          <div *ngIf="!stageId" class="stage-selector">
            <h3>Sélectionner un stage pour créer une réclamation</h3>
            <div class="stage-list">
              <div *ngIf="stages.length === 0" class="empty-state">
                <p>Aucun stage actif trouvé. Vous devez d'abord avoir un stage en cours.</p>
              </div>
              <button *ngFor="let stage of stages" class="stage-card" (click)="selectStage(stage.id)">
                <div class="stage-info">
                  <h4>{{ stage.titre }}</h4>
                  <p>{{ stage.entrepriseNom }}</p>
                </div>
                <span class="arrow">→</span>
              </button>
            </div>
          </div>

          <div *ngIf="stageId" class="reclamations-list">
            <button class="btn btn-secondary back-btn" (click)="deselectStage()">
              ← Retour
            </button>
            <header class="reclamations-header-inner">
              <div>
                <p class="selected-stage">Stage sélectionné: {{ selectedStageTitle }}</p>
              </div>
              <button class="btn btn-primary" (click)="openForm()">
                + Nouvelle réclamation
              </button>
            </header>

            <div *ngIf="reclamations.length === 0" class="empty-state">
              <div class="empty-icon">📋</div>
              <p>Aucune réclamation enregistrée pour ce stage.</p>
              <button class="btn btn-primary" (click)="openForm()">
                Créer ma première réclamation
              </button>
            </div>

            <div *ngFor="let reclamation of reclamations" class="reclamation-card">
              <div class="card-header">
                <h3 class="card-title">{{ reclamation.objet }}</h3>
                <span class="status-badge" [ngClass]="'status-' + reclamation.statut">
                  {{ getStatutLabel(reclamation.statut) }}
                </span>
              </div>

              <div class="card-body">
                <div class="info-row">
                  <span class="info-label">Type:</span>
                  <span class="info-value">{{ getTypeLabel(reclamation.typeReclamation) }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Créée:</span>
                  <span class="info-value">{{ reclamation.dateCreation | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
                <div class="info-row" *ngIf="reclamation.nomTraitePar">
                  <span class="info-label">Traitée par:</span>
                  <span class="info-value">{{ reclamation.nomTraitePar }}</span>
                </div>
                <div class="info-row" *ngIf="reclamation.dateCloture">
                  <span class="info-label">Clôturée:</span>
                  <span class="info-value">{{ reclamation.dateCloture | date:'dd/MM/yyyy HH:mm' }}</span>
                </div>
              </div>

              <div class="card-footer">
                <a [routerLink]="['/espace-etudiant/reclamations', reclamation.id]" class="btn btn-secondary">
                  Voir le détail
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <app-reclamation-form
      *ngIf="showForm"
      [stageId]="stageId"
      (formClosed)="closeForm()"
      (reclamationCreated)="onReclamationCreated()"
    ></app-reclamation-form>
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
      margin-bottom: 40px;
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
    }

    .portal-nav a {
      color: #4f46e5;
      text-decoration: none;
      font-weight: 600;
      transition: color 0.2s;
    }

    .portal-nav a:hover {
      color: #4338ca;
    }

    .reclamations-container {
      max-width: 1000px;
    }

    .reclamations-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 40px;
      gap: 20px;
      flex-wrap: wrap;
    }

    .page-title {
      font-size: 2.2rem;
      font-weight: 800;
      color: #111827;
      margin: 0 0 8px 0;
      background: linear-gradient(90deg, #1f2937, #4b5563);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .page-subtitle {
      color: #6b7280;
      font-size: 1.1rem;
      margin: 0;
    }

    .alert {
      padding: 16px 20px;
      border-radius: 12px;
      margin-bottom: 24px;
    }

    .alert-error {
      background: #fef2f2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }

    .reclamations-list {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .empty-state {
      text-align: center;
      padding: 60px 40px;
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    }

    .empty-icon {
      font-size: 3rem;
      margin-bottom: 16px;
    }

    .empty-state p {
      color: #6b7280;
      font-size: 1.1rem;
      margin-bottom: 24px;
    }

    .reclamation-card {
      background: white;
      border-radius: 12px;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
      overflow: hidden;
      transition: box-shadow 0.2s, transform 0.2s;
    }

    .reclamation-card:hover {
      box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }

    .card-header {
      padding: 20px;
      background: linear-gradient(135deg, #f9fafb, #f3f4f6);
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #e5e7eb;
    }

    .card-title {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 700;
      color: #1f2937;
    }

    .status-badge {
      padding: 6px 14px;
      border-radius: 999px;
      font-size: 0.85rem;
      font-weight: 600;
      text-transform: uppercase;
      white-space: nowrap;
    }

    .status-OUVERTE { background: #dbeafe; color: #1e40af; }
    .status-EN_TRAITEMENT { background: #fed7aa; color: #92400e; }
    .status-RESOLUE { background: #d1fae5; color: #065f46; }
    .status-CLOTUREE { background: #e5e7eb; color: #374151; }
    .status-REOUVERTE { background: #fee2e2; color: #7f1d1d; }

    .card-body {
      padding: 20px;
    }

    .info-row {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;
      font-size: 0.95rem;
    }

    .info-row:last-child {
      margin-bottom: 0;
    }

    .info-label {
      font-weight: 600;
      color: #6b7280;
      min-width: 100px;
    }

    .info-value {
      color: #374151;
    }

    .card-footer {
      padding: 16px 20px;
      border-top: 1px solid #e5e7eb;
      background: #f9fafb;
    }

    .btn {
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      font-size: 0.95rem;
      transition: all 0.2s;
    }

    .btn-primary {
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      color: white;
      box-shadow: 0 4px 6px -1px rgba(99, 102, 241, 0.2);
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.3);
    }

    .btn-secondary {
      background: white;
      color: #6366f1;
      border: 1px solid #e5e7eb;
    }

    .stage-selector {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
      margin-bottom: 24px;
    }

    .stage-selector h3 {
      margin: 0 0 20px 0;
      font-size: 1.25rem;
      color: #1f2937;
    }

    .stage-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .stage-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      text-align: left;
    }

    .stage-card:hover {
      background: #f3f4f6;
      border-color: #6366f1;
      box-shadow: 0 4px 6px -1px rgba(99, 102, 241, 0.1);
    }

    .stage-info h4 {
      margin: 0 0 4px 0;
      color: #1f2937;
      font-size: 1rem;
    }

    .stage-info p {
      margin: 0;
      color: #6b7280;
      font-size: 0.9rem;
    }

    .arrow {
      color: #6366f1;
      font-size: 1.2rem;
    }

    .back-btn {
      margin-bottom: 20px;
    }

    .reclamations-header-inner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      gap: 20px;
      flex-wrap: wrap;
    }

    .selected-stage {
      color: #6b7280;
      font-weight: 600;
      margin: 0;
    }
  `]
})
export class ReclamationListComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private reclamationService = inject(ReclamationService);
  private etudiantStageService = inject(EtudiantStageService);

  stageId!: number;
  selectedStageTitle = '';
  reclamations: Reclamation[] = [];
  stages: any[] = [];
  showForm = false;
  errorMessage = '';

  ngOnInit(): void {
    const stageIdParam = this.route.snapshot.queryParamMap.get('stageId');
    if (stageIdParam) {
      this.stageId = +stageIdParam;
      this.loadReclamations();
    } else {
      this.loadStages();
    }
  }

  loadStages(): void {
    this.errorMessage = '';
    this.etudiantStageService.listerCandidatures().subscribe({
      next: (data) => {
        // Extraire les candidatures acceptées (qui représentent les "stages")
        this.stages = data.content
          .filter(candidature => candidature.statut === 'ACCEPTEE')
          .map(candidature => ({
            id: candidature.id, // C'est l'ID de la candidature, utilisé comme stageId
            titre: candidature.offreTitre || 'Offre',
            entrepriseNom: candidature.entrepriseNom || 'Entreprise'
          }));
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message || 'Erreur lors du chargement des candidatures';
      }
    });
  }

  selectStage(stageId: number): void {
    this.stageId = stageId;
    const stage = this.stages.find(s => s.id === stageId);
    this.selectedStageTitle = stage ? `${stage.titre} (${stage.entrepriseNom})` : '';
    this.loadReclamations();
  }

  deselectStage(): void {
    this.stageId = 0;
    this.selectedStageTitle = '';
    this.reclamations = [];
    this.loadStages();
  }

  loadReclamations(): void {
    if (!this.stageId) return;
    this.errorMessage = '';
    this.reclamationService.listerReclamations(this.stageId).subscribe({
      next: (data) => {
        this.reclamations = data.sort((a, b) =>
          new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime()
        );
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message || 'Erreur lors du chargement des réclamations';
      }
    });
  }

  openForm(): void {
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onReclamationCreated(): void {
    this.loadReclamations();
  }

  getTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'NOTE': 'Note',
      'DOCUMENT': 'Document',
      'EVALUATION': 'Évaluation',
      'AUTRE': 'Autre'
    };
    return labels[type] || type;
  }

  getStatutLabel(statut: StatutReclamation): string {
    const labels: { [key in StatutReclamation]: string } = {
      'OUVERTE': 'Ouverte',
      'EN_TRAITEMENT': 'En traitement',
      'RESOLUE': 'Résolue',
      'CLOTUREE': 'Clôturée',
      'REOUVERTE': 'Réouverte'
    };
    return labels[statut] || '';
  }
}
