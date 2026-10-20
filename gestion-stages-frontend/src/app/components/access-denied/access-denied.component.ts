import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="denied-page">
      <section class="denied-card">
        <div class="denied-icon">🛡️</div>
        <p class="denied-code">Erreur 403</p>
        <h1>Accès refusé</h1>
        <p class="denied-desc">Votre profil ou rôle actuel ne dispose pas des privilèges nécessaires pour accéder à cette ressource.</p>
        <a class="button-link" routerLink="/login">Retour à la connexion</a>
      </section>
    </main>
  `,
  styles: `
    .denied-page {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 2.5rem 1.5rem;
      background:
        radial-gradient(900px 500px at 15% 15%, rgba(239, 68, 68, 0.06), transparent 60%),
        radial-gradient(800px 450px at 85% 85%, rgba(37, 99, 235, 0.06), transparent 50%),
        #f8fafc;
    }
    .denied-card {
      width: min(100%, 30rem);
      padding: 3rem 2.25rem;
      text-align: center;
      border-radius: 1.25rem;
      background: #ffffff;
      border: 1px solid rgba(226, 232, 240, 0.9);
      box-shadow: 0 20px 40px -8px rgba(15, 23, 42, 0.08);
    }
    .denied-icon {
      font-size: 3rem;
      margin-bottom: 0.75rem;
    }
    .denied-code {
      margin: 0 0 0.5rem;
      font-size: 0.8rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: #dc2626;
    }
    h1 {
      margin: 0 0 0.75rem;
      font-size: 1.85rem;
      font-weight: 800;
      letter-spacing: -0.025em;
      color: #0f172a;
    }
    .denied-desc {
      margin: 0 0 2rem;
      color: #64748b;
      font-size: 0.95rem;
      line-height: 1.5;
    }
    .button-link {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 2.75rem;
      padding: 0 1.5rem;
      border-radius: 0.65rem;
      color: #ffffff;
      background: linear-gradient(135deg, #2563eb, #3b82f6);
      font-weight: 700;
      font-size: 0.925rem;
      text-decoration: none;
      box-shadow: 0 4px 14px -1px rgba(37, 99, 235, 0.35);
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .button-link:hover {
      background: linear-gradient(135deg, #1d4ed8, #2563eb);
      transform: translateY(-1px);
      box-shadow: 0 6px 18px 0 rgba(37, 99, 235, 0.45);
    }
  `,
})
export class AccessDeniedComponent {}
