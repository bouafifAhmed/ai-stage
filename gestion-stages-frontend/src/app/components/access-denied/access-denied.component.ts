import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main>
      <section>
        <h1>Accès refusé</h1>
        <p>Votre rôle ne permet pas d’accéder à cette page.</p>
        <a routerLink="/login">Retour à la connexion</a>
      </section>
    </main>
  `,
  styles: `
    main {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 2rem;
      background: #f8fafc;
    }
    section {
      text-align: center;
    }
    a {
      color: #0369a1;
      font-weight: 700;
    }
  `,
})
export class AccessDeniedComponent {}
