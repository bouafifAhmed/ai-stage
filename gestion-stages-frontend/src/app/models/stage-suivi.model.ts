import { StatutTacheAssignee } from './tache-assignee.model';

export type StatutStage = 'ACTIF' | 'CLOTURE';

export interface EcheanceCalendrier {
  tacheId: number;
  stageId: number;
  titre: string;
  dateEcheance: string;
  statut: StatutTacheAssignee;
  nomEtudiant: string;
  entrepriseNom: string;
  offreTitre: string;
}

export interface StageCloture {
  stageId: number;
  statutStage: StatutStage;
  signatureEtudiantPresente: boolean;
  signatureEncadrantPresente: boolean;
  dateSignatureEtudiant: string | null;
  dateSignatureEncadrant: string | null;
  dateCloture: string | null;
  nomEtudiant: string;
  nomEncadrant: string | null;
  entrepriseNom: string;
  offreTitre: string;
  peutSignerEtudiant: boolean;
  peutSignerEncadrant: boolean;
}

export interface SignerStageRequest {
  signatureBase64: string;
}
