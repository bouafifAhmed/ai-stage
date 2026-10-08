import { PageResponse } from './entreprise.model';

export type StatutOffre = 'BROUILLON' | 'PUBLIEE' | 'CLOTUREE' | 'ARCHIVEE';
export type ModeTravail = 'PRESENTIEL' | 'HYBRIDE' | 'DISTANCIEL';
export type StatutCandidature =
  | 'EN_ATTENTE'
  | 'ACCEPTEE'
  | 'REFUSEE';

export interface OffreStage {
  id: number;
  titre: string;
  description: string;
  entrepriseId?: number;
  entrepriseNom?: string;
  localisation: string;
  mode: ModeTravail;
  domaine: string;
  typeStage: string;
  dureeMois: number | null;
  dateDebut: string | null;
  dateFin?: string | null;
  remuneration?: number | null;
  competences: string[];
  niveauRequis?: string | null;
  nombrePlaces: number;
  placesOccupees: number;
  placesRestantes: number;
  statut?: StatutOffre;
  dateCreation: string;
  dateModification: string;
  dateLimite: string;
}

export interface OffreStageRequest {
  titre: string;
  description: string;
  localisation: string;
  mode: ModeTravail;
  domaine: string;
  typeStage: string;
  dureeMois?: number;
  dateDebut?: string;
  dateFin?: string;
  remuneration?: number;
  competences: string[];
  niveauRequis?: string;
  nombrePlaces: number;
  statut: StatutOffre;
  dateLimite: string;
}

export interface Candidature {
  id: number;
  offreId: number;
  offreTitre?: string;
  entrepriseNom?: string;
  etudiantId?: number;
  etudiantNom?: string;
  etudiantPrenom?: string;
  etudiantEmail?: string;
  telephone?: string;
  filiere?: string;
  niveauEtudes?: string;
  competences?: string[];
  message: string;
  statut: StatutCandidature;
  dateCandidature: string;
}

export interface CreateCandidatureRequest {
  offreId: number;
  message: string;
}

export type TypeNotification = 'CANDIDATURE_ACCEPTEE' | 'TACHE_ASSIGNEE';

export interface NotificationEtudiant {
  id: number;
  candidatureId: number;
  offreId: number;
  offreTitre: string;
  entrepriseNom: string;
  message: string;
  lue: boolean;
  dateCreation: string;
  type: TypeNotification;
  tacheId?: number | null;
}

export type StagePage<T> = PageResponse<T>;
