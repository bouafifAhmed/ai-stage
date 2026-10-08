import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  Recommandation,
  RecommandationAffichage,
  AdequationAnalyse
} from '../models/recommandation.model';

@Injectable({
  providedIn: 'root'
})
export class RecommandationService {

  private apiUrl = `${environment.apiUrl}/recommandations`;

  constructor(private http: HttpClient) { }

  /**
   * Obtient les offres recommandées pour l'étudiant connecté.
   *
   * Effectue un GET sur /api/recommandations et retourne les recommandations triées par pertinence.
   *
   * Gestion d'erreur :
   * - 401 Unauthorized : Utilisateur non authentifié ou JWT expiré
   * - 403 Forbidden : Utilisateur n'a pas le rôle ETUDIANT
   * - 404 Not Found : Étudiant non trouvé
   * - 500 Internal Server Error : Erreur serveur
   * - Timeout ou erreur réseau : Retourner une liste vide (fonctionnalité optionnelle)
   *
   * @returns Observable<Recommandation[]> : Liste des offres recommandées
   *
   * Exemple de réponse :
   * [
   *   {"offreId": 3, "nomEntreprise": "TechCorp", "score": 0.87},
   *   {"offreId": 1, "nomEntreprise": "DataLabs", "score": 0.65},
   *   ...
   * ]
   */
  obtenirRecommandations(): Observable<Recommandation[]> {
    return this.http.get<Recommandation[]>(this.apiUrl).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur lors de la récupération des recommandations :', error);

        // Dégradation gracieuse : retourner une liste vide
        // (les recommandations sont une fonctionnalité additive, pas critique)
        if (error.status === 401 || error.status === 403) {
          console.warn('Authentification échouée ou permissions insuffisantes');
        } else if (error.status === 404) {
          console.warn('Ressource non trouvée');
        } else if (error.status === 0) {
          console.warn('Impossible de se connecter au serveur');
        }

        return of([]);
      })
    );
  }

  /**
   * Obtient les recommandations avec les scores enrichis en pourcentage.
   *
   * Exemple de transformation :
   * IN  : {"offreId": 1, "nomEntreprise": "TechCorp", "score": 0.87}
   * OUT : {..., "scorePercentage": 87}
   *
   * @returns Observable<RecommandationAffichage[]>
   */
  obtenirRecommandationsAffichage(): Observable<RecommandationAffichage[]> {
    return this.obtenirRecommandations().pipe(
      map((recommandations: Recommandation[]) =>
        recommandations.map((rec: Recommandation) => ({
          ...rec,
          scorePercentage: Math.round(rec.score * 100)
        }))
      )
    );
  }

  /**
   * Vérifie que le service de recommandation Python est accessible.
   * Endpoint optionnel pour le diagnostique.
   *
   * @returns Observable<boolean>
   */
  healthCheck(): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/health`).pipe(
      catchError(() => of(false))
    );
  }

  /**
   * Analyse détaillée d'adéquation et d'écart de compétences (Skill Gap) pour une offre.
   */
  obtenirAdequation(offreId: number): Observable<AdequationAnalyse | null> {
    return this.http.get<AdequationAnalyse>(`${this.apiUrl}/adequation/${offreId}`).pipe(
      catchError((error) => {
        console.warn('Erreur lors de la récupération de l\'analyse d\'adéquation', error);
        return of(null);
      })
    );
  }
}
