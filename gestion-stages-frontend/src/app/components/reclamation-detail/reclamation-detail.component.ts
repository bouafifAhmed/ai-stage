import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Params, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ReclamationService } from '../../services/reclamation.service';
import { ReclamationDetail } from '../../models/reclamation.model';
import { ReclamationThreadComponent } from '../reclamation-thread/reclamation-thread.component';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reclamation-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReclamationThreadComponent],
  template: `
    <main class="portal-page">
      <section class="portal-shell">
        <header class="portal-nav">
          <a class="brand" [routerLink]="homeLink">Gestion des stages</a>
          <nav>
            <ng-container *ngIf="isEtudiant; else entrepriseNav">
              <a routerLink="/espace-etudiant">Offres</a>
              <a routerLink="/espace-etudiant/reclamations" [queryParams]="listQueryParams">Mes réclamations</a>
              <a routerLink="/espace-etudiant/profil">Mon profil</a>
            </ng-container>
            <ng-template #entrepriseNav>
              <a routerLink="/espace-entreprise">Mes offres</a>
              <a routerLink="/espace-entreprise/reclamations">Réclamations</a>
            </ng-template>
          </nav>
        </header>

        <div class="detail-container">
          <button class="btn btn-back" [routerLink]="listLink" [queryParams]="listQueryParams">
            ← Retour
          </button>

          <div *ngIf="errorMessage" class="alert alert-error">
            {{ errorMessage }}
          </div>

          <div *ngIf="isLoading" class="loading-state">
            Chargement...
          </div>

          <app-reclamation-thread
            *ngIf="!isLoading && reclamation"
            [reclamation]="reclamation"
            [canManage]="canManage"
            [isEtudiant]="isEtudiant"
            [currentUserId]="currentUserId"
            (reclamationUpdated)="loadDetail()"
          ></app-reclamation-thread>
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
      margin-bottom: 30px;
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

    .detail-container {
      max-width: 900px;
    }

    .btn-back {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      margin-bottom: 20px;
      background: white;
      color: #6366f1;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      text-decoration: none;
    }

    .btn-back:hover {
      background: #f9fafb;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
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

    .loading-state {
      text-align: center;
      padding: 60px 40px;
      background: white;
      border-radius: 12px;
      color: #6b7280;
      font-size: 1.1rem;
    }
  `]
})
export class ReclamationDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private reclamationService = inject(ReclamationService);
  private authService = inject(AuthService);

  reclamation?: ReclamationDetail;
  stageId!: number;
  currentUserId?: number;
  isEtudiant = false;
  canManage = false;
  isLoading = false;
  errorMessage = '';
  homeLink = '/espace-etudiant';
  listLink = '/espace-etudiant/reclamations';
  listQueryParams: Params | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const stageIdParam = this.route.snapshot.queryParamMap.get('stageId');

    if (id) {
      const reclamationId = +id;
      this.stageId = stageIdParam ? +stageIdParam : 0;

      // Récupérer l'utilisateur connecté
      this.authService.getCurrentUser().subscribe({
        next: (user) => {
          if (user) {
            this.currentUserId = user.id;
            const role = user.role || '';
            this.isEtudiant = role === 'ETUDIANT';
            this.canManage = ['CHEF_DEPT_STAGE', 'CHEF_DEPT_PEDAGOGIQUE', 'ENTREPRISE'].includes(role);
          } else {
            this.isEtudiant = !this.router.url.startsWith('/espace-entreprise');
          }
          this.configureNavigation();
          this.loadDetail();
        },
        error: () => {
          this.isEtudiant = !this.router.url.startsWith('/espace-entreprise');
          this.configureNavigation();
          this.loadDetail();
        }
      });
    }
  }

  private configureNavigation(): void {
    if (this.isEtudiant) {
      this.homeLink = '/espace-etudiant';
      this.listLink = '/espace-etudiant/reclamations';
      this.listQueryParams = this.stageId ? { stageId: this.stageId } : null;
      return;
    }

    this.homeLink = '/espace-entreprise';
    this.listLink = '/espace-entreprise/reclamations';
    this.listQueryParams = null;
  }

  loadDetail(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.reclamationService.getDetail(+id).subscribe({
      next: (data) => {
        this.reclamation = data;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Erreur lors du chargement de la réclamation';
      }
    });
  }
}
