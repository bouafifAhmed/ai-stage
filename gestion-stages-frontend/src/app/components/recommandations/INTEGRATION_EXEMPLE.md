# 📦 Intégration du Composant Recommandations

Ce fichier montre comment intégrer le composant `RecommandationsComponent` dans votre application Angular.

## 🎯 1. Importer le Composant Standalone

Puisque `RecommandationsComponent` est un composant **standalone**, vous pouvez l'importer directement dans n'importe quel autre composant ou module.

### Option A : Dans un composant parent (standalone ou non)

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecommandationsComponent } from './components/recommandations/recommandations.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RecommandationsComponent],
  template: `
    <div class="dashboard">
      <h1>Mon Tableau de Bord Étudiant</h1>
      
      <!-- Afficher les recommandations -->
      <app-recommandations></app-recommandations>
      
      <!-- Autres sections -->
      <section class="autres-offres">
        <h2>Autres Offres</h2>
        <!-- ... -->
      </section>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 2rem;
    }
  `]
})
export class DashboardComponent { }
```

### Option B : Dans un module (si vous utilisez encore les modules)

```typescript
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardComponent } from './dashboard.component';
import { RecommandationsComponent } from './components/recommandations/recommandations.component';

@NgModule({
  declarations: [DashboardComponent],
  imports: [
    CommonModule,
    RecommandationsComponent  // ← Importer le composant standalone
  ]
})
export class DashboardModule { }
```

---

## 📍 2. Placer le Composant dans le Template

### Emplacement recommandé

```html
<!-- src/app/components/espace-etudiant/espace-etudiant.component.html -->

<div class="espace-etudiant-container">
  <!-- Header -->
  <header class="espace-header">
    <h1>🎓 Mon Espace Étudiant</h1>
    <p class="welcome-message">Bienvenue, {{ etudiant.prenom }}!</p>
  </header>

  <!-- Section 1 : Recommandations (NOUVELLE) -->
  <section class="section-recommandations">
    <app-recommandations></app-recommandations>
  </section>

  <!-- Section 2 : Mes Candidatures -->
  <section class="section-candidatures">
    <h2>📋 Mes Candidatures</h2>
    <app-mes-candidatures></app-mes-candidatures>
  </section>

  <!-- Section 3 : Toutes les Offres -->
  <section class="section-offres">
    <h2>💼 Toutes les Offres</h2>
    <app-offres-list></app-offres-list>
  </section>

  <!-- Section 4 : Réclamations -->
  <section class="section-reclamations">
    <h2>📝 Mes Réclamations</h2>
    <app-reclamation-list></app-reclamation-list>
  </section>
</div>
```

### Styles pour l'intégration

```css
/* src/app/components/espace-etudiant/espace-etudiant.component.css */

.espace-etudiant-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.espace-header {
  text-align: center;
  margin-bottom: 3rem;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 2rem;
}

.espace-header h1 {
  font-size: 2rem;
  color: #333;
  margin: 0;
}

.welcome-message {
  color: #666;
  font-size: 1rem;
  margin-top: 0.5rem;
}

/* Sections */
section {
  margin-bottom: 3rem;
  background: #f9f9f9;
  padding: 2rem;
  border-radius: 0.5rem;
  border-left: 4px solid #3498db;
}

section h2 {
  margin-top: 0;
  color: #333;
}

.section-recommandations {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-left-color: #667eea;
}

.section-candidatures {
  border-left-color: #27ae60;
}

.section-offres {
  border-left-color: #e74c3c;
}

.section-reclamations {
  border-left-color: #f39c12;
}
```

---

## 🧩 3. Composant Parent Complet (Exemple)

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecommandationsComponent } from './components/recommandations/recommandations.component';
import { EtudiantService } from './services/etudiant.service';

@Component({
  selector: 'app-espace-etudiant',
  standalone: true,
  imports: [CommonModule, RecommandationsComponent],
  templateUrl: './espace-etudiant.component.html',
  styleUrls: ['./espace-etudiant.component.css']
})
export class EspaceEtudiantComponent implements OnInit {
  etudiant: any;
  isLoading = true;

  constructor(private etudiantService: EtudiantService) { }

  ngOnInit(): void {
    this.chargerProfil();
  }

  chargerProfil(): void {
    this.etudiantService.obtenirMonProfil().subscribe({
      next: (data) => {
        this.etudiant = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement du profil :', err);
        this.isLoading = false;
      }
    });
  }
}
```

---

## 🔄 4. Intégration avec Routage

Si vous avez des routes pour l'espace étudiant :

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { EspaceEtudiantComponent } from './components/espace-etudiant/espace-etudiant.component';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'espace-etudiant',
    component: EspaceEtudiantComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ETUDIANT'] }
  },
  {
    path: 'espace-etudiant/offres-recommandees',
    component: EspaceEtudiantComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ETUDIANT'] }
  },
  // ... autres routes
];
```

---

## 📱 5. Responsive Design - Intégration Mobile

Le composant est déjà responsive, mais vous pouvez l'adapter :

```html
<!-- Version mobile : afficher seulement les recommandations en haut -->
<div class="mobile-layout">
  <!-- Recommandations (priorité haute sur mobile) -->
  <app-recommandations></app-recommandations>

  <!-- Offres en grid resserré -->
  <app-offres-list [layout]="'compact'"></app-offres-list>
</div>
```

```css
@media (max-width: 768px) {
  .espace-etudiant-container {
    padding: 1rem 0.5rem;
  }

  section {
    padding: 1rem;
    margin-bottom: 2rem;
  }

  section h2 {
    font-size: 1.3rem;
  }
}
```

---

## 🎨 6. Personnalisation du Style

### Thème personnalisé

```css
/* Adapter les couleurs du composant recommandations */

/* Override les couleurs du score */
:host ::ng-deep {
  .score-high {
    background-color: #10b981; /* Vert personnalisé */
  }

  .score-medium {
    background-color: #f59e0b; /* Orange personnalisé */
  }

  .score-low {
    background-color: #ef4444; /* Rouge personnalisé */
  }

  .btn-details {
    background-color: #your-color;
  }
}
```

---

## 🔌 7. Communication avec d'autres Composants

### Exemple : Sélectionner une recommandation et la mettre en évidence

```typescript
// espace-etudiant.component.ts

export class EspaceEtudiantComponent {
  selectedOffreId: number | null = null;

  selectOffre(offreId: number): void {
    this.selectedOffreId = offreId;
    // Scroll vers l'offre
    document.getElementById(`offre-${offreId}`)?.scrollIntoView({ behavior: 'smooth' });
  }
}
```

```html
<!-- espace-etudiant.component.html -->

<app-recommandations 
  (onSelectOffre)="selectOffre($event)">
</app-recommandations>

<!-- Offres avec highlight -->
<div class="offres-container">
  <div *ngFor="let offre of offres"
       [id]="'offre-' + offre.id"
       [class.highlight]="selectedOffreId === offre.id"
       class="offre-card">
    {{ offre.nom }}
  </div>
</div>
```

**Note** : Le composant `RecommandationsComponent` actuel n'émet pas d'événements. Vous pouvez le modifier si vous avez besoin :

```typescript
// recommandations.component.ts
@Output() onSelectOffre = new EventEmitter<number>();

onClickOffre(offreId: number): void {
  this.onSelectOffre.emit(offreId);
}
```

---

## 🧪 8. Tests Unitaires

```typescript
// recommandations.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RecommandationsComponent } from './recommandations.component';
import { RecommandationService } from '../../services/recommandation.service';

describe('RecommandationsComponent', () => {
  let component: RecommandationsComponent;
  let fixture: ComponentFixture<RecommandationsComponent>;
  let service: RecommandationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecommandationsComponent, HttpClientTestingModule],
      providers: [RecommandationService]
    }).compileComponents();

    fixture = TestBed.createComponent(RecommandationsComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(RecommandationService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load recommendations on init', () => {
    spyOn(service, 'obtenirRecommandationsAffichage').and.returnValue(
      of([
        { offreId: 1, nomEntreprise: 'Test', score: 0.87, scorePercentage: 87 }
      ])
    );

    component.ngOnInit();

    expect(component.recommandations.length).toBe(1);
    expect(component.isLoading).toBeFalse();
  });

  it('should handle error gracefully', () => {
    spyOn(service, 'obtenirRecommandationsAffichage').and.returnValue(
      throwError(() => new Error('Test error'))
    );

    component.ngOnInit();

    expect(component.hasError).toBeTrue();
    expect(component.recommandations.length).toBe(0);
  });
});
```

---

## 📋 9. Checklist d'Intégration

- [ ] Importer `RecommandationsComponent` dans le composant parent
- [ ] Ajouter `<app-recommandations></app-recommandations>` au template
- [ ] Vérifier que le service `RecommandationService` est fourni
- [ ] Tester en navigateur (F12)
- [ ] Vérifier les logs du backend
- [ ] Adapter les styles si nécessaire
- [ ] Tester sur mobile (F12 → toggle device)
- [ ] Tester les différents états (chargement, erreur, données)
- [ ] Écrire les tests unitaires

---

## 🚀 10. Déploiement

Une fois intégré, le composant se comportera automatiquement :

```bash
# Build pour production
ng build --configuration production

# Output : dist/gestion-stages-frontend
```

Le composant :
- ✅ Se charge au montage du composant parent
- ✅ Récupère les recommandations via le backend
- ✅ Affiche les résultats ou un message d'erreur
- ✅ Gère les timeouts gracieusement
- ✅ Est responsive sur tous les appareils

---

**Version 1.0 - 15 Septembre 2026**
**Composant Recommandations - Guide d'Intégration**
