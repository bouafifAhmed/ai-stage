import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RecommandationService } from '../../services/recommandation.service';
import { RecommandationAffichage } from '../../models/recommandation.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

/**
 * Composant standalone Angular affichant les offres recommandées pour l'étudiant connecté.
 *
 * Fonctionnalités :
 * 1. Récupère les recommandations au chargement du composant
 * 2. Affiche une liste de cartes avec :
 *    - Nom de l'entreprise
 *    - Score de pertinence (pourcentage et barre visuelle)
 * 3. Gère les états :
 *    - Chargement (spinner)
 *    - Données disponibles (affichage des cartes)
 *    - Pas de données (message "Aucune recommandation")
 * 4. Gère les erreurs gracieusement (dégradation gracieuse)
 *
 * Styling :
 * - Utilise CSS/SCSS pour les cartes, barres de progression, etc.
 * - Design responsive (mobile-friendly)
 * - Accessibilité : labels clairs, contraste adéquat
 *
 * Performance :
 * - Utilise OnDestroy et takeUntil pour se désabonner des observables
 * - Prévient les memory leaks
 */
@Component({
  selector: 'app-recommandations',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './recommandations.component.html',
  styleUrls: ['./recommandations.component.css']
})
export class RecommandationsComponent implements OnInit, OnDestroy {

  // Données
  recommandations: RecommandationAffichage[] = [];

  // États UI
  isLoading = true;
  hasError = false;
  errorMessage = '';

  // Pour la destruction du composant
  private destroy$ = new Subject<void>();

  constructor(private recommandationService: RecommandationService) { }

  ngOnInit(): void {
    this.chargerRecommandations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Charge les recommandations depuis le service.
   */
  private chargerRecommandations(): void {
    this.isLoading = true;
    this.hasError = false;

    this.recommandationService.obtenirRecommandationsAffichage()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (recommandations: RecommandationAffichage[]) => {
          this.recommandations = recommandations;
          this.isLoading = false;
          console.log(`${recommandations.length} recommandations chargées`);
        },
        error: (error) => {
          console.error('Erreur lors du chargement des recommandations :', error);
          this.isLoading = false;
          this.hasError = true;
          this.errorMessage = 'Impossible de charger les recommandations';
          // Dégradation gracieuse : ne pas bloquer l'utilisateur
        }
      });
  }

  /**
   * Réessaye de charger les recommandations (après une erreur).
   */
  retryCharger(): void {
    this.chargerRecommandations();
  }

  /**
   * Retourne la couleur d'une barre de progression en fonction du score.
   * - 0.0 - 0.33 : Rouge (faible pertinence)
   * - 0.34 - 0.66 : Orange (pertinence moyenne)
   * - 0.67 - 1.0 : Vert (pertinence élevée)
   *
   * @param score Score entre 0 et 1
   * @returns Classe CSS correspondante
   */
  getCouleurScore(score: number): string {
    if (score >= 0.67) {
      return 'score-high';
    } else if (score >= 0.34) {
      return 'score-medium';
    } else {
      return 'score-low';
    }
  }

  /**
   * Retourne la description textuelle du score.
   * @param score Score entre 0 et 1
   * @returns Description lisible
   */
  getDescriptionScore(score: number): string {
    if (score >= 0.67) {
      return 'Excellente correspondance';
    } else if (score >= 0.34) {
      return 'Bonne correspondance';
    } else {
      return 'Faible correspondance';
    }
  }
}
