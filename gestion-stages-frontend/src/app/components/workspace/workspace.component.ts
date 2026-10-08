import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-workspace',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="workspace">
      <section>
        <p class="eyebrow">Gestion des stages</p>
        <h1>{{ title }}</h1>
        @if (user(); as currentUser) {
          <p>
            Bienvenue {{ currentUser.prenom }} {{ currentUser.nom }}.
          </p>
          <p class="role">Rôle : {{ currentUser.role }}</p>
          @if (currentUser.role === 'SUPER_ADMIN') {
            <a class="management-link" routerLink="/admin/entreprises">
              Gestion des entreprises
            </a>
            <a class="management-link" routerLink="/admin/etudiants">
              Gestion des étudiants
            </a>
          }
        }
        <button type="button" (click)="logout()">Se déconnecter</button>
      </section>
    </main>
  `,
  styles: `
    .workspace {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 2rem;
      background: #f1f5f9;
      color: #0f172a;
    }
    section {
      width: min(100%, 42rem);
      padding: 2rem;
      border-radius: 1rem;
      background: #fff;
      box-shadow: 0 1rem 2.5rem rgba(15, 23, 42, 0.1);
    }
    .eyebrow {
      color: #0369a1;
      font-weight: 700;
    }
    .role {
      color: #475569;
    }
    .management-link {
      display: inline-block;
      margin: 0 0.75rem 1rem 0;
      color: #0369a1;
      font-weight: 700;
    }
    button {
      padding: 0.75rem 1rem;
      border: 0;
      border-radius: 0.5rem;
      color: #fff;
      background: #0369a1;
      font: inherit;
      font-weight: 700;
      cursor: pointer;
    }
  `,
})
export class WorkspaceComponent {
  private readonly authService = inject(AuthService);
  readonly title = inject(ActivatedRoute).snapshot.data['title'] as string;
  readonly user = toSignal(this.authService.getCurrentUser(), {
    initialValue: this.authService.currentUserSnapshot(),
  });

  logout(): void {
    this.authService.logout();
  }
}
