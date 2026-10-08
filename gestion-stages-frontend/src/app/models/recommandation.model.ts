/**
 * Modèles TypeScript pour les recommandations d'offres de stage.
 * Miroir exact des DTOs du backend Spring Boot pour assurer la compatibilité JSON.
 */

/**
 * Une recommandation individuelle : une offre avec son score de pertinence.
 *
 * Champs :
 * - offreId : Identifiant de l'offre recommandée
 * - nomEntreprise : Nom de l'entreprise (pour affichage)
 * - score : Score de similarité cosinus entre 0 et 1, arrondi à 2 décimales
 */
export interface Recommandation {
  offreId: number;
  nomEntreprise: string;
  score: number;
}

/**
 * Résumé d'une recommandation avec score en pourcentage (pour l'UI).
 */
export interface RecommandationAffichage extends Recommandation {
  scorePercentage: number; // score * 100 (ex: 0.87 → 87%)
}

/**
 * Analyse d'adéquation et d'écart de compétences (Skill Gap) pour une offre.
 */
export interface AdequationAnalyse {
  offreId: number;
  score: number;
  scorePourcentage: number;
  competencesAcquises: string[];
  competencesManquantes: string[];
  conseils: string[];
}

