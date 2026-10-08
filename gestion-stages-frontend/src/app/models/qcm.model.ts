export interface QcmOption {
  id: number;
  libelle: string;
  ordre: number;
}

export interface QcmQuestion {
  id: number;
  enonce: string;
  type: 'CHOIX_UNIQUE' | 'CHOIX_MULTIPLE' | 'VRAI_FAUX';
  points: number;
  ordre: number;
  options: QcmOption[];
}

export interface QcmExamen {
  id: number;
  titre: string;
  domaine: string;
  dureeMinutes: number;
  noteMinimalePassage: number;
  nombreMaxTentatives: number;
  questions: QcmQuestion[];
}

export interface QcmStatut {
  qcmId: number;
  titre: string;
  admissible: boolean;
  reussi: boolean;
  tentativesUtilisees: number;
  tentativesRestantes: number;
  meilleurScore: number | null;
}

export interface ReponseQuestionRequest {
  questionId: number;
  optionId: number;
}

export interface SoumettreQcmRequest {
  reponses: ReponseQuestionRequest[];
}

export interface SoumettreQcmResponse {
  scorePourcentage: number;
  reussi: boolean;
  noteMinimalePassage: number;
  tentativesUtilisees: number;
  tentativesRestantes: number;
  admissible: boolean;
  message: string;
}
