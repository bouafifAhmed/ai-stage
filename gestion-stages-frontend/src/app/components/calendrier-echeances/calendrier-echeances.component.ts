import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { EcheanceCalendrier } from '../../models/stage-suivi.model';
import { AuthService } from '../../services/auth.service';
import { StageSuiviService } from '../../services/stage-suivi.service';

@Component({
  selector: 'app-calendrier-echeances',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="portal-page"><section class="portal-shell">
      <header class="portal-nav">
        <a class="brand" [routerLink]="homeLink">Gestion des stages</a>
        <nav>
          @if (role === 'ETUDIANT') {
            <a routerLink="/espace-etudiant">Offres</a>
            <a routerLink="/espace-etudiant/candidatures">Mes candidatures</a>
          } @else {
            <a routerLink="/espace-entreprise">Mes offres</a>
          }
          @if (stageId) {
            <a [routerLink]="tachesLink">Tâches</a>
            <a [routerLink]="clotureLink">Clôture</a>
          }
        </nav>
      </header>

      <div class="page-header">
        <div>
          <p class="eyebrow">Suivi du stage</p>
          <h1>Calendrier des échéances</h1>
          <p>Vue mensuelle des dates limites des tâches assignées.</p>
        </div>
        <div class="month-nav">
          <button type="button" class="button" (click)="previousMonth()">Mois précédent</button>
          <strong>{{ monthLabel }}</strong>
          <button type="button" class="button" (click)="nextMonth()">Mois suivant</button>
        </div>
      </div>

      @if (errorMessage) { <div class="alert" role="alert">{{ errorMessage }}</div> }
      @if (loading) { <div class="card empty">Chargement du calendrier…</div> }
      @else {
        <div class="calendar card">
          <div class="weekdays">
            @for (day of weekdays; track day) { <div>{{ day }}</div> }
          </div>
          <div class="grid">
            @for (cell of cells; track cell.key) {
              <div class="cell" [class.outside]="!cell.inMonth" [class.today]="cell.isToday">
                <div class="day-number">{{ cell.day }}</div>
                @for (item of cell.items; track item.tacheId) {
                  <div class="event" [class]="'status-' + item.statut.toLowerCase()">
                    <strong>{{ item.titre }}</strong>
                    @if (!stageId) {
                      <small>{{ item.offreTitre }}</small>
                    }
                  </div>
                }
              </div>
            }
          </div>
        </div>
      }
    </section></main>
  `,
  styles: `
    .month-nav { display: flex; align-items: center; gap: .75rem; flex-wrap: wrap; }
    .calendar { padding: 1rem; }
    .weekdays, .grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: .5rem; }
    .weekdays { margin-bottom: .5rem; font-weight: 700; text-align: center; color: #475569; }
    .cell { min-height: 6.5rem; border: 1px solid #e2e8f0; border-radius: .5rem; padding: .35rem; background: #fff; }
    .cell.outside { background: #f8fafc; color: #94a3b8; }
    .cell.today { border-color: #2563eb; box-shadow: inset 0 0 0 1px #2563eb; }
    .day-number { font-weight: 700; margin-bottom: .25rem; }
    .event { margin-top: .25rem; padding: .25rem .35rem; border-radius: .35rem; font-size: .75rem; line-height: 1.2; }
    .event strong { display: block; }
    .status-assignee { background: #e2e8f0; }
    .status-en_cours { background: #fef3c7; }
    .status-terminee { background: #dbeafe; }
    .status-validee { background: #dcfce7; }
    .status-rejetee { background: #fee2e2; }
    @media (max-width: 48rem) {
      .weekdays, .grid { gap: .25rem; }
      .cell { min-height: 5rem; font-size: .85rem; }
    }
  `,
})
export class CalendrierEcheancesComponent implements OnInit {
  private readonly service = inject(StageSuiviService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readonly stageId = this.route.snapshot.paramMap.get('stageId')
    ? Number(this.route.snapshot.paramMap.get('stageId'))
    : null;
  readonly role = this.route.snapshot.data['role'] as 'ETUDIANT' | 'ENTREPRISE';
  readonly homeLink = this.role === 'ETUDIANT' ? '/espace-etudiant' : '/espace-entreprise';
  readonly tachesLink = this.stageId
    ? (this.role === 'ETUDIANT'
      ? ['/espace-etudiant/stages', this.stageId, 'taches-assignees']
      : ['/espace-entreprise/stages', this.stageId, 'taches-assignees', 'validation'])
    : null;
  readonly clotureLink = this.stageId
    ? (this.role === 'ETUDIANT'
      ? ['/espace-etudiant/stages', this.stageId, 'cloture']
      : ['/espace-entreprise/stages', this.stageId, 'cloture'])
    : null;

  readonly weekdays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  currentYear = new Date().getFullYear();
  currentMonth = new Date().getMonth() + 1;
  echeances: EcheanceCalendrier[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    if (!this.isBrowser) {
      this.loading = false;
      return;
    }
    if (!this.authService.isAuthenticated()) {
      this.loading = false;
      this.errorMessage = 'Veuillez vous connecter pour consulter le calendrier.';
      return;
    }
    this.load();
  }

  get monthLabel(): string {
    return new Date(this.currentYear, this.currentMonth - 1, 1)
      .toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }

  get cells(): CalendarCell[] {
    const first = new Date(this.currentYear, this.currentMonth - 1, 1);
    const startOffset = (first.getDay() + 6) % 7;
    const daysInMonth = new Date(this.currentYear, this.currentMonth, 0).getDate();
    const grouped = this.groupByDay(this.echeances);
    const today = new Date();
    const cells: CalendarCell[] = [];

    for (let i = 0; i < startOffset; i += 1) {
      const day = new Date(this.currentYear, this.currentMonth - 1, -startOffset + i + 1).getDate();
      cells.push({
        key: `prev-${i}`,
        day,
        inMonth: false,
        isToday: false,
        items: [],
      });
    }

    for (let day = 1; day <= daysInMonth; day += 1) {
      const isToday = today.getFullYear() === this.currentYear
        && today.getMonth() + 1 === this.currentMonth
        && today.getDate() === day;
      cells.push({
        key: `current-${day}`,
        day,
        inMonth: true,
        isToday,
        items: grouped.get(day) ?? [],
      });
    }

    while (cells.length % 7 !== 0) {
      const index = cells.length - (startOffset + daysInMonth);
      cells.push({
        key: `next-${index}`,
        day: index + 1,
        inMonth: false,
        isToday: false,
        items: [],
      });
    }

    return cells;
  }

  previousMonth(): void {
    if (this.currentMonth === 1) {
      this.currentMonth = 12;
      this.currentYear -= 1;
    } else {
      this.currentMonth -= 1;
    }
    this.load();
  }

  nextMonth(): void {
    if (this.currentMonth === 12) {
      this.currentMonth = 1;
      this.currentYear += 1;
    } else {
      this.currentMonth += 1;
    }
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.errorMessage = '';
    const request = this.stageId
      ? this.service.echeancesStage(this.stageId, this.currentYear, this.currentMonth)
      : this.service.echeancesEtudiant(this.currentYear, this.currentMonth);

    request.subscribe({
      next: (items) => {
        this.echeances = items;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.message ?? 'Impossible de charger le calendrier.';
        this.loading = false;
      },
    });
  }

  private groupByDay(items: EcheanceCalendrier[]): Map<number, EcheanceCalendrier[]> {
    const map = new Map<number, EcheanceCalendrier[]>();
    for (const item of items) {
      const day = new Date(item.dateEcheance).getDate();
      const list = map.get(day) ?? [];
      list.push(item);
      map.set(day, list);
    }
    return map;
  }
}

interface CalendarCell {
  key: string;
  day: number;
  inMonth: boolean;
  isToday: boolean;
  items: EcheanceCalendrier[];
}
