import { isPlatformBrowser } from '@angular/common';
import { Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TacheService } from '../../services/tache.service';
import { Tache, Progression } from '../../models/tache.model';
import { ProgressionBarComponent } from '../progression-bar/progression-bar.component';
import { TacheFormComponent } from '../tache-form/tache-form.component';

@Component({
  selector: 'app-journal-stage',
  standalone: true,
  imports: [CommonModule, RouterLink, ProgressionBarComponent, TacheFormComponent],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav">
        <a class="brand" routerLink="/espace-etudiant">Gestion des stages</a>
        <nav>
          <a routerLink="/espace-etudiant">Offres</a>
          <a routerLink="/espace-etudiant/candidatures">Mes candidatures</a>
          <a routerLink="/espace-etudiant/notifications">Notifications</a>
          <a routerLink="/espace-etudiant/profil">Mon profil</a>
          <a *ngIf="stageId" [routerLink]="['/espace-etudiant/stages', stageId, 'taches-assignees']">Tâches</a>
          <a *ngIf="stageId" [routerLink]="['/espace-etudiant/stages', stageId, 'calendrier']">Calendrier</a>
          <a *ngIf="stageId" [routerLink]="['/espace-etudiant/stages', stageId, 'cloture']">Clôture</a>
        </nav>
      </header>
      <div class="journal-container">
        <header class="journal-header">
          <div>
            <h1 class="journal-title">Mon Journal de Stage</h1>
            <p class="journal-subtitle">Documentez vos tâches quotidiennes et suivez votre progression.</p>
          </div>
          <div class="header-actions">
            <button class="btn-pdf" (click)="downloadPdf()" [disabled]="downloadingPdf">
              📄 {{ downloadingPdf ? 'Téléchargement...' : 'Télécharger PDF' }}
            </button>
            <button class="btn-primary" (click)="openForm()">+ Ajouter une entrée</button>
          </div>
        </header>
      <div *ngIf="errorMessage" class="alert">{{ errorMessage }}</div>

      <app-progression-bar *ngIf="progression" [pourcentage]="progression.pourcentage"></app-progression-bar>

      <div class="timeline">
        <div *ngIf="taches.length === 0" class="empty-state">
          Aucune tâche enregistrée pour le moment.
        </div>
        
        <div class="timeline-item" *ngFor="let tache of taches">
          <div class="timeline-date">
            <span class="date-day">{{ tache.date | date:'dd' }}</span>
            <span class="date-month">{{ tache.date | date:'MMM' }}</span>
          </div>
          <div class="timeline-content">
            <div class="tache-header">
              <h3 class="tache-titre">{{ tache.titre }}</h3>
            </div>
            
            <p class="tache-desc">{{ tache.description }}</p>
            
            <div *ngIf="tache.pieceJointe" class="attachment">
              <button type="button" class="attachment-link" (click)="downloadPieceJointe(tache)">
                📎 Pièce jointe
              </button>
            </div>

            <div *ngIf="tache.statutApprobation === 'REJETEE'" class="rejection-box">
              <p class="feedback-title">Commentaire de l'encadrant :</p>
              <p class="feedback-text">{{ tache.commentaireEncadrant || 'Aucun commentaire.' }}</p>
              <button class="btn-secondary" (click)="openForm(tache)">Corriger l'entrée</button>
            </div>
          </div>
        </div>
      </div>
    </div>
    </section></main>

    <app-tache-form *ngIf="showForm" 
      [stageId]="stageId" 
      [tache]="selectedTache"
      (formClosed)="closeForm()"
      (taskSaved)="onTaskSaved()">
    </app-tache-form>
  `,
  styles: [`
    .journal-container {
      max-width: 900px; margin: 0 auto; padding: 40px 20px;
      font-family: 'Inter', sans-serif;
    }
    .journal-nav { display: flex; gap: 1rem; margin-bottom: 1.5rem; }
    .journal-nav a { color: #4f46e5; font-weight: 700; text-decoration: none; }
    .alert {
      background: #fef2f2; color: #991b1b; border: 1px solid #fecaca;
      padding: 12px 16px; border-radius: 8px; margin-bottom: 20px;
    }
    .journal-header {
      display: flex; justify-content: space-between; align-items: flex-start;
      margin-bottom: 40px; gap: 20px; flex-wrap: wrap;
    }
    .header-actions {
      display: flex; gap: 12px; align-items: center;
    }
    .journal-title {
      font-size: 2.2rem; font-weight: 800; color: #111827; margin: 0 0 8px 0;
      background: linear-gradient(90deg, #1f2937, #4b5563);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    }
    .journal-subtitle {
      color: #6b7280; font-size: 1.1rem; margin: 0;
    }
    .btn-primary {
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      color: white; border: none; padding: 12px 24px; border-radius: 999px;
      font-weight: 600; font-size: 1rem; cursor: pointer;
      transition: all 0.2s; box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.2);
    }
    .btn-primary:hover {
      transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.3);
    }
    .btn-pdf {
      background: linear-gradient(135deg, #10b981, #059669);
      color: white; border: none; padding: 12px 24px; border-radius: 999px;
      font-weight: 600; font-size: 1rem; cursor: pointer;
      transition: all 0.2s; box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.2);
    }
    .btn-pdf:hover:not([disabled]) {
      transform: translateY(-2px); box-shadow: 0 10px 15px -3px rgba(16, 185, 129, 0.3);
    }
    .btn-pdf[disabled] {
      opacity: 0.6; cursor: not-allowed;
    }
    .btn-secondary {
      background: #fff; color: #dc2626; border: 1px solid #fca5a5;
      padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer;
      margin-top: 12px; transition: background 0.2s;
    }
    .btn-secondary:hover { background: #fef2f2; }
    
    .timeline { position: relative; margin-top: 40px; }
    .timeline::before {
      content: ''; position: absolute; top: 0; bottom: 0; left: 24px;
      width: 2px; background: #e5e7eb;
    }
    .timeline-item {
      display: flex; margin-bottom: 30px; position: relative; z-index: 1;
    }
    .timeline-date {
      width: 50px; height: 50px; background: #fff; border: 2px solid #6366f1;
      border-radius: 50%; display: flex; flex-direction: column;
      align-items: center; justify-content: center; margin-right: 20px;
      flex-shrink: 0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
    }
    .date-day { font-weight: 800; font-size: 1.1rem; color: #111827; line-height: 1; }
    .date-month { font-size: 0.7rem; color: #6b7280; text-transform: uppercase; font-weight: 700; }
    
    .timeline-content {
      background: #fff; border-radius: 16px; padding: 24px; flex-grow: 1;
      box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); border: 1px solid #f3f4f6;
      transition: box-shadow 0.2s;
    }
    .timeline-content:hover { box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); }
    .tache-header {
      display: flex; justify-content: space-between; align-items: flex-start;
      margin-bottom: 12px;
    }
    .tache-titre {
      margin: 0; font-size: 1.25rem; font-weight: 700; color: #1f2937;
    }
    .status-badge {
      padding: 4px 12px; border-radius: 999px; font-size: 0.75rem; font-weight: 700;
    }
    .en_attente { background: #fef3c7; color: #d97706; }
    .approuvee { background: #d1fae5; color: #059669; }
    .rejetee { background: #fee2e2; color: #dc2626; }
    
    .tache-desc { color: #4b5563; line-height: 1.6; margin-bottom: 16px; white-space: pre-line; }
    
    .attachment { margin-bottom: 16px; }
    .attachment-link {
      color: #3b82f6; text-decoration: none; font-weight: 500; font-size: 0.9rem;
      background: #eff6ff; padding: 6px 12px; border-radius: 6px; border: none; cursor: pointer;
      display: inline-flex; align-items: center; gap: 4px;
    }
    .attachment-link:hover { background: #dbeafe; }
    
    .rejection-box {
      background: #fff5f5; border-left: 4px solid #ef4444; padding: 16px;
      border-radius: 0 8px 8px 0; margin-top: 16px;
    }
    .feedback-title { font-weight: 700; color: #991b1b; margin: 0 0 4px 0; font-size: 0.9rem; }
    .feedback-text { color: #b91c1c; margin: 0; font-size: 0.95rem; font-style: italic; }
    
    .empty-state { text-align: center; color: #9ca3af; padding: 40px; font-style: italic; }
  `]
})
export class JournalStageComponent implements OnInit {
  private readonly tacheService = inject(TacheService);
  private readonly route = inject(ActivatedRoute);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  stageId!: number;
  taches: Tache[] = [];
  progression?: Progression;
  showForm = false;
  selectedTache?: Tache;
  errorMessage = '';
  downloadingPdf = false;

  ngOnInit(): void {
    if (!this.isBrowser) {
      return;
    }
    const idParam = this.route.snapshot.paramMap.get('stageId');
    if (idParam) {
      this.stageId = +idParam;
      this.loadData();
    }
  }

  loadData() {
    this.errorMessage = '';
    this.tacheService.listerTaches(this.stageId).subscribe({
      next: (data) => {
        this.taches = [...data].sort(
          (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
        );
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage =
          error.error?.message ?? 'Impossible de charger le journal de stage.';
      },
    });
    this.tacheService.getProgression(this.stageId).subscribe({
      next: (data) => {
        this.progression = data;
      },
      error: (error: HttpErrorResponse) => {
        if (!this.errorMessage) {
          this.errorMessage =
            error.error?.message ?? 'Impossible de charger la progression.';
        }
      },
    });
  }

  downloadJournalPdf(): void {
    this.downloadPdf();
  }

  downloadPdf(): void {
    if (!this.stageId) return;
    this.downloadingPdf = true;
    this.tacheService.telechargerJournalPdf(this.stageId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        // Open PDF in a new tab so the browser handles display/download
        window.open(url, '_blank');
        // Clean up the object URL after a short delay
        setTimeout(() => window.URL.revokeObjectURL(url), 5000);
        this.downloadingPdf = false;
      },
      error: (error: HttpErrorResponse) => {
        this.downloadingPdf = false;
        alert(error.error?.message ?? 'Erreur lors du téléchargement du PDF.');
      },
    });
  }

  downloadPieceJointe(tache: Tache): void {
    if (!tache.id) return;
    this.tacheService.telechargerPieceJointe(this.stageId, tache.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (error: HttpErrorResponse) => {
        alert(error.error?.message ?? 'Impossible de charger la pièce jointe.');
      },
    });
  }

  formatStatus(status: string): string {
    const map: any = {
      'EN_ATTENTE': 'En attente',
      'APPROUVEE': 'Approuvée',
      'REJETEE': 'À corriger'
    };
    return map[status] || status;
  }

  openForm(tache?: Tache) {
    this.selectedTache = tache;
    this.showForm = true;
  }

  closeForm() {
    this.showForm = false;
    this.selectedTache = undefined;
  }

  onTaskSaved() {
    this.closeForm();
    this.loadData();
  }
}
