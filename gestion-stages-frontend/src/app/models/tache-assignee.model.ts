export type StatutTacheAssignee =
  | 'ASSIGNEE'
  | 'EN_COURS'
  | 'TERMINEE'
  | 'VALIDEE'
  | 'REJETEE';

export interface TacheAssignee {
  id: number;
  stageId: number;
  encadrantId: number;
  nomEncadrant: string;
  etudiantId: number;
  nomEtudiant: string;
  titre: string;
  description: string;
  statut: StatutTacheAssignee;
  dateEcheance: string | null;
  dateAssignation: string;
  dateCompletion: string | null;
  dateValidation: string | null;
  commentaireEtudiant: string | null;
  pieceJointeEtudiant: string | null;
  commentaireEncadrant: string | null;
}

export interface AssignerTacheRequest {
  titre: string;
  description: string;
  dateEcheance: string | null;
}

export interface DecisionTacheRequest {
  commentaireEncadrant: string;
}
