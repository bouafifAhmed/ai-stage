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

      <!-- Hero header card -->
      <div class="journal-hero">
        <div class="journal-hero-left">
          <span class="eyebrow">Mon espace</span>
          <h1 class="journal-title">Journal de Stage</h1>
          <p class="journal-subtitle">Documentez vos tâches quotidiennes et suivez votre progression.</p>
        </div>
        <div class="journal-hero-right">
          <div class="stat-pill" *ngIf="taches.length > 0">
            <span class="stat-number">{{ taches.length }}</span>
            <span class="stat-label">entrée{{ taches.length > 1 ? 's' : '' }}</span>
          </div>
          <button class="btn-pdf" (click)="downloadPdf()" [disabled]="downloadingPdf">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            {{ downloadingPdf ? 'Téléchargement...' : 'Export PDF' }}
          </button>
          <button class="btn-primary" (click)="openForm()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Nouvelle entrée
          </button>
        </div>
      </div>

      <div *ngIf="errorMessage" class="alert">{{ errorMessage }}</div>

      <app-progression-bar *ngIf="progression" [pourcentage]="progression.pourcentage"></app-progression-bar>

      <!-- Timeline -->
      <div class="timeline">
        <div *ngIf="taches.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          </div>
          <p class="empty-title">Aucune entrée pour l'instant</p>
          <p class="empty-hint">Commencez par ajouter votre première tâche journalière.</p>
          <button class="btn-primary" (click)="openForm()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Ajouter ma première entrée
          </button>
        </div>

        <div class="timeline-item" *ngFor="let tache of taches">
          <div class="timeline-node">
            <div class="timeline-date">
              <span class="date-day">{{ tache.date | date:'dd' }}</span>
              <span class="date-month">{{ tache.date | date:'MMM' }}</span>
            </div>
          </div>
          <div class="timeline-content"
            [class.is-approved]="tache.statutApprobation === 'APPROUVEE'"
            [class.is-rejected]="tache.statutApprobation === 'REJETEE'">
            <div class="tache-header">
              <h3 class="tache-titre">{{ tache.titre }}</h3>
              <span class="status-badge"
                [class.badge-pending]="tache.statutApprobation === 'EN_ATTENTE'"
                [class.badge-approved]="tache.statutApprobation === 'APPROUVEE'"
                [class.badge-rejected]="tache.statutApprobation === 'REJETEE'">
                <span class="badge-dot"></span>
                {{ formatStatus(tache.statutApprobation) }}
              </span>
            </div>

            <p class="tache-desc">{{ tache.description }}</p>

            <div *ngIf="tache.pieceJointe" class="attachment">
              <button type="button" class="attachment-link" (click)="downloadPieceJointe(tache)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
                Pièce jointe
              </button>
            </div>

            <div *ngIf="tache.statutApprobation === 'REJETEE'" class="rejection-box">
              <p class="feedback-title">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                Commentaire de l'encadrant
              </p>
              <p class="feedback-text">{{ tache.commentaireEncadrant || 'Aucun commentaire.' }}</p>
              <button class="btn-correct" (click)="openForm(tache)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                Corriger l'entrée
              </button>
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
    /* ── Hero Header ── */
    .journal-hero {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1.5rem;
      margin-bottom: 2rem;
      padding: 2rem 2.25rem;
      background: linear-gradient(135deg, rgba(37,99,235,0.06) 0%, rgba(6,182,212,0.04) 100%);
      border: 1px solid rgba(37,99,235,0.12);
      border-radius: 1.25rem;
      position: relative;
      overflow: hidden;
    }
    .journal-hero::before {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(135deg, rgba(255,255,255,0.75) 0%, rgba(255,255,255,0.35) 100%);
      pointer-events: none;
    }
    .journal-hero > * { position: relative; }
    .journal-hero-left { flex: 1; min-width: 200px; }
    .journal-hero-right {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
    }
    .journal-title {
      font-size: 1.9rem;
      font-weight: 800;
      letter-spacing: -0.03em;
      margin: 0.25rem 0 0.5rem;
      background: linear-gradient(135deg, #1e40af 0%, #2563eb 50%, #0891b2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      line-height: 1.2;
    }
    .journal-subtitle {
      color: #64748b;
      font-size: 0.95rem;
      margin: 0;
    }
    /* Stat pill */
    .stat-pill {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 0.45rem 1rem;
      background: rgba(255,255,255,0.9);
      border: 1px solid rgba(37,99,235,0.15);
      border-radius: 0.75rem;
      box-shadow: 0 2px 8px rgba(37,99,235,0.08);
    }
    .stat-number {
      font-size: 1.4rem;
      font-weight: 800;
      color: #2563eb;
      line-height: 1;
    }
    .stat-label {
      font-size: 0.68rem;
      font-weight: 700;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    /* ── Buttons ── */
    .btn-primary {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      background: linear-gradient(135deg, #2563eb, #4f46e5);
      color: #fff;
      border: none;
      padding: 0.6rem 1.2rem;
      border-radius: 999px;
      font-weight: 700;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 4px 14px -2px rgba(37,99,235,0.3);
      white-space: nowrap;
    }
    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px -2px rgba(37,99,235,0.4);
    }
    .btn-pdf {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      background: linear-gradient(135deg, #10b981, #059669);
      color: #fff;
      border: none;
      padding: 0.6rem 1.2rem;
      border-radius: 999px;
      font-weight: 700;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 4px 14px -2px rgba(16,185,129,0.25);
      white-space: nowrap;
    }
    .btn-pdf:hover:not([disabled]) {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px -2px rgba(16,185,129,0.35);
    }
    .btn-pdf[disabled] { opacity: 0.6; cursor: not-allowed; }
    .btn-correct {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      background: #fff;
      color: #dc2626;
      border: 1.5px solid #fca5a5;
      padding: 0.45rem 0.9rem;
      border-radius: 0.5rem;
      font-weight: 700;
      font-size: 0.85rem;
      cursor: pointer;
      margin-top: 0.75rem;
      transition: all 0.15s;
    }
    .btn-correct:hover { background: #fef2f2; border-color: #f87171; }
    /* ── Timeline ── */
    .timeline { position: relative; margin-top: 2rem; }
    .timeline::before {
      content: '';
      position: absolute;
      top: 26px;
      bottom: 0;
      left: 26px;
      width: 2px;
      background: linear-gradient(to bottom, rgba(37,99,235,0.3), rgba(6,182,212,0.15), transparent);
    }
    .timeline-item {
      display: flex;
      gap: 1.25rem;
      margin-bottom: 1.5rem;
      position: relative;
      z-index: 1;
    }
    .timeline-node { flex-shrink: 0; }
    .timeline-date {
      width: 52px;
      height: 52px;
      background: #fff;
      border: 2px solid #2563eb;
      border-radius: 50%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 0 4px rgba(37,99,235,0.07), 0 4px 12px -2px rgba(37,99,235,0.15);
      transition: transform 0.2s;
    }
    .timeline-item:hover .timeline-date { transform: scale(1.07); }
    .date-day { font-weight: 800; font-size: 1.05rem; color: #1e40af; line-height: 1; }
    .date-month { font-size: 0.65rem; color: #64748b; text-transform: uppercase; font-weight: 700; letter-spacing: 0.05em; }
    /* ── Cards ── */
    .timeline-content {
      background: #fff;
      border-radius: 1rem;
      padding: 1.4rem 1.6rem;
      flex-grow: 1;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 8px -1px rgba(15,23,42,0.05);
      transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
    }
    .timeline-content:hover {
      box-shadow: 0 8px 24px -4px rgba(15,23,42,0.1);
      transform: translateY(-1px);
      border-color: #cbd5e1;
    }
    .timeline-content.is-approved { border-left: 3px solid #10b981; }
    .timeline-content.is-rejected { border-left: 3px solid #ef4444; }
    .tache-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 0.75rem;
      margin-bottom: 0.65rem;
      flex-wrap: wrap;
    }
    .tache-titre { margin: 0; font-size: 1.05rem; font-weight: 700; color: #1e293b; letter-spacing: -0.01em; }
    .tache-desc { color: #475569; line-height: 1.65; margin: 0 0 0.5rem; white-space: pre-line; font-size: 0.92rem; }
    /* ── Status badges ── */
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.28rem 0.75rem;
      border-radius: 999px;
      font-size: 0.74rem;
      font-weight: 700;
      letter-spacing: 0.02em;
      white-space: nowrap;
    }
    .badge-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
    .badge-pending { background: #fef3c7; color: #b45309; border: 1px solid rgba(180,83,9,0.18); }
    .badge-pending .badge-dot { background: #f59e0b; }
    .badge-approved { background: #dcfce7; color: #15803d; border: 1px solid rgba(21,128,61,0.18); }
    .badge-approved .badge-dot { background: #10b981; }
    .badge-rejected { background: #fee2e2; color: #b91c1c; border: 1px solid rgba(185,28,28,0.18); }
    .badge-rejected .badge-dot { background: #ef4444; }
    /* ── Attachment ── */
    .attachment { margin: 0.65rem 0 0; }
    .attachment-link {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      color: #2563eb;
      background: #eff6ff;
      border: 1px solid rgba(37,99,235,0.15);
      padding: 0.38rem 0.8rem;
      border-radius: 0.5rem;
      font-weight: 600;
      font-size: 0.875rem;
      cursor: pointer;
      transition: all 0.15s;
    }
    .attachment-link:hover { background: #dbeafe; border-color: rgba(37,99,235,0.3); }
    /* ── Rejection box ── */
    .rejection-box {
      background: #fff5f5;
      border-left: 3px solid #ef4444;
      border-radius: 0 0.5rem 0.5rem 0;
      padding: 0.9rem 1.1rem;
      margin-top: 1rem;
    }
    .feedback-title {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-weight: 700;
      color: #991b1b;
      margin: 0 0 0.35rem;
      font-size: 0.85rem;
    }
    .feedback-text { color: #b91c1c; margin: 0; font-size: 0.9rem; font-style: italic; line-height: 1.55; }
    /* ── Empty state ── */
    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
      background: #fff;
      border: 1.5px dashed #cbd5e1;
      border-radius: 1.25rem;
    }
    .empty-icon {
      width: 68px;
      height: 68px;
      margin: 0 auto 1.25rem;
      background: linear-gradient(135deg, #eff6ff, #e0f2fe);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #2563eb;
    }
    .empty-title { font-size: 1.1rem; font-weight: 700; color: #1e293b; margin: 0 0 0.4rem; }
    .empty-hint { color: #64748b; font-size: 0.9rem; margin: 0 0 1.5rem; }
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
