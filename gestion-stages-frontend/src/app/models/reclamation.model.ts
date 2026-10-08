export type TypeReclamation = 'NOTE' | 'DOCUMENT' | 'EVALUATION' | 'AUTRE';
export type StatutReclamation = 'OUVERTE' | 'EN_TRAITEMENT' | 'RESOLUE' | 'CLOTUREE' | 'REOUVERTE';

export interface MessageReclamation {
  id: number;
  contenu: string;
  pieceJointe?: string;
  dateEnvoi: string;
  nomAuteur: string;
  roleAuteur: string;
}

export interface Reclamation {
  id: number;
  typeReclamation: TypeReclamation;
  objet: string;
  statut: StatutReclamation;
  dateCreation: string;
  dateCloture?: string;
  nomEtudiant: string;
  nomTraitePar?: string;
}

export interface ReclamationDetail extends Reclamation {
  messages: MessageReclamation[];
}
