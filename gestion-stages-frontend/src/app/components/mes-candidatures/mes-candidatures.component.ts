import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Candidature, StatutCandidature } from '../../models/stage.model';
import { EtudiantStageService } from '../../services/etudiant-stage.service';

@Component({
  selector: 'app-mes-candidatures',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav"><a class="brand" routerLink="/espace-etudiant">Gestion des stages</a><nav><a routerLink="/espace-etudiant">Offres</a><a routerLink="/espace-etudiant/candidatures">Mes candidatures</a><a routerLink="/espace-etudiant/calendrier">Calendrier global</a><a routerLink="/espace-etudiant/notifications">Notifications</a><a routerLink="/espace-etudiant/profil">Mon profil</a></nav></header>
      <div class="page-header"><div><p class="eyebrow">Espace étudiant</p><h1>Mes candidatures</h1><p>Suivez l'avancement de vos demandes.</p></div></div>
      @if (errorMessage) { <div class="alert">{{ errorMessage }}</div> }
      <div class="table-wrapper"><table>
        <thead><tr><th>Offre</th><th>Date</th><th>Message</th><th>Statut</th><th>Actions</th></tr></thead>
        <tbody>
          @if (loading) { <tr><td colspan="5" class="empty">Chargement…</td></tr> }
          @else if (!candidatures.length) { <tr><td colspan="5" class="empty">Vous n'avez encore envoyé aucune candidature.</td></tr> }
          @else {
            @for (item of candidatures; track item.id) {
              <tr>
                <td><strong>{{ item.offreTitre || ('Offre #' + item.offreId) }}</strong><small>{{ item.entrepriseNom }}</small></td>
                <td>{{ item.dateCandidature | date: 'dd/MM/yyyy' }}</td>
                <td class="message-cell">{{ item.message }}</td>
                <td><span class="badge" [class]="'status-' + item.statut.toLowerCase()">{{ statusLabel(item.statut) }}</span></td>
                <td>
                  @if (item.statut === 'ACCEPTEE') {
                    <div class="actions">
                      <a class="task-link" [routerLink]="['/espace-etudiant/stages', item.id, 'taches-assignees']">Tâches</a>
                      <a class="task-link" [routerLink]="['/espace-etudiant/stages', item.id, 'journal']">Journal</a>
                      <a class="task-link" [routerLink]="['/espace-etudiant/stages', item.id, 'calendrier']">Calendrier</a>
                      <a class="task-link" [routerLink]="['/espace-etudiant/stages', item.id, 'cloture']">Clôture</a>
                    </div>
                  } @else { <span class="muted">—</span> }
                </td>
              </tr>
            }
          }
        </tbody>
      </table></div>
    </section></main>
  `,
  styles: `.actions { display: flex; flex-wrap: wrap; gap: .5rem; } .task-link { font-weight: 700; white-space: nowrap; }`,
})
export class MesCandidaturesComponent implements OnInit {
  private readonly service = inject(EtudiantStageService);
  candidatures: Candidature[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.service.listerCandidatures().subscribe({
      next: (result) => { this.candidatures = result.content; this.loading = false; },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger vos candidatures.';
        this.loading = false;
      },
    });
  }

  statusLabel(status: StatutCandidature): string {
    const labels: Record<StatutCandidature, string> = {
      EN_ATTENTE: 'En attente', ACCEPTEE: 'Acceptée', REFUSEE: 'Refusée',
    };
    return labels[status];
  }
}
