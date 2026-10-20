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
        <header class="workspace-header">
          <div class="workspace-brand">
            <span class="brand-badge">StageConnect Administration</span>
            <h1>{{ title }}</h1>
          </div>
          @if (user(); as currentUser) {
            <div class="user-pill">
              <div class="user-avatar">{{ (currentUser.prenom || currentUser.nom || 'A')[0].toUpperCase() }}</div>
              <div>
                <strong>{{ currentUser.prenom }} {{ currentUser.nom }}</strong>
                <span class="role-tag">{{ currentUser.role }}</span>
              </div>
            </div>
          }
        </header>

        <div class="workspace-body">
          @if (user(); as currentUser) {
            @if (currentUser.role === 'SUPER_ADMIN') {
              <div class="modules-grid">
                <div class="module-card">
                  <div class="module-icon">🏢</div>
                  <h3>Entreprises partenaires</h3>
                  <p>Validation, modération et suivi des entreprises partenaires et offres associées.</p>
                  <a class="management-link" routerLink="/admin/entreprises">
                    Gestion des entreprises →
                  </a>
                </div>
                <div class="module-card">
                  <div class="module-icon">🎓</div>
                  <h3>Comptes étudiants</h3>
                  <p>Gestion des profils étudiants, suivi des filières, compétences et activation.</p>
                  <a class="management-link" routerLink="/admin/etudiants">
                    Gestion des étudiants →
                  </a>
                </div>
              </div>
            } @else {
              <div class="department-notice">
                <p>Bienvenue dans votre tableau de bord de coordination des stages universitaires.</p>
              </div>
            }
          }
        </div>

        <footer class="workspace-footer">
          <button type="button" (click)="logout()">
            <span>Se déconnecter</span>
          </button>
        </footer>
      </section>
    </main>
  `,
  styles: `
    .workspace {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 2.5rem 1.5rem;
      background:
        radial-gradient(1000px 500px at 15% 10%, rgba(37, 99, 235, 0.08), transparent 60%),
        radial-gradient(800px 450px at 85% 90%, rgba(6, 182, 212, 0.08), transparent 50%),
        #f8fafc;
      color: #0f172a;
    }
    section {
      width: min(100%, 48rem);
      padding: 2.5rem;
      border-radius: 1.25rem;
      background: rgba(255, 255, 255, 0.98);
      border: 1px solid rgba(226, 232, 240, 0.9);
      box-shadow: 0 20px 40px -8px rgba(15, 23, 42, 0.08), 0 1px 3px 0 rgba(15, 23, 42, 0.04);
    }
    .workspace-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1.5rem;
      padding-bottom: 1.75rem;
      border-bottom: 1px solid #f1f5f9;
      flex-wrap: wrap;
    }
    .brand-badge {
      display: inline-block;
      margin-bottom: 0.35rem;
      font-size: 0.75rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #2563eb;
    }
    h1 {
      margin: 0;
      font-size: 1.75rem;
      font-weight: 800;
      letter-spacing: -0.025em;
      color: #0f172a;
    }
    .user-pill {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.5rem 0.85rem;
      border-radius: 9999px;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
    }
    .user-avatar {
      display: grid;
      place-items: center;
      width: 2.25rem;
      height: 2.25rem;
      border-radius: 50%;
      background: linear-gradient(135deg, #2563eb, #3b82f6);
      color: #ffffff;
      font-weight: 800;
      font-size: 0.95rem;
    }
    .user-pill strong {
      display: block;
      font-size: 0.88rem;
      color: #0f172a;
    }
    .role-tag {
      font-size: 0.72rem;
      font-weight: 700;
      color: #2563eb;
    }
    .workspace-body {
      margin: 2rem 0;
    }
    .modules-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 1.25rem;
    }
    .module-card {
      padding: 1.5rem;
      border-radius: 1rem;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      display: flex;
      flex-direction: column;
    }
    .module-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 10px 24px -2px rgba(37, 99, 235, 0.1);
      border-color: #cbd5e1;
    }
    .module-icon {
      font-size: 1.75rem;
      margin-bottom: 0.75rem;
    }
    .module-card h3 {
      margin: 0 0 0.5rem;
      font-size: 1.15rem;
      font-weight: 700;
      color: #0f172a;
    }
    .module-card p {
      margin: 0 0 1.25rem;
      color: #64748b;
      font-size: 0.88rem;
      line-height: 1.45;
      flex: 1;
    }
    .management-link {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      color: #2563eb;
      font-weight: 700;
      font-size: 0.925rem;
      text-decoration: none;
      transition: all 0.2s ease;
    }
    .management-link:hover {
      color: #1d4ed8;
      transform: translateX(3px);
    }
    .department-notice {
      padding: 1.5rem;
      border-radius: 0.75rem;
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #1e40af;
    }
    .workspace-footer {
      display: flex;
      justify-content: flex-end;
      padding-top: 1.5rem;
      border-top: 1px solid #f1f5f9;
    }
    button {
      padding: 0.65rem 1.25rem;
      border: 1px solid #cbd5e1;
      border-radius: 0.65rem;
      color: #334155;
      background: #ffffff;
      font: inherit;
      font-size: 0.9rem;
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    button:hover {
      background: #fee2e2;
      border-color: #fecaca;
      color: #b91c1c;
    }
    @media (max-width: 40rem) {
      .modules-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class WorkspaceComponent {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly title: string = this.route.snapshot.data['title'] ?? 'Espace d\'administration';

  readonly user = toSignal(this.authService.getCurrentUser(), {
    initialValue: this.authService.currentUserSnapshot(),
  });

  logout(): void {
    this.authService.logout();
  }
}
