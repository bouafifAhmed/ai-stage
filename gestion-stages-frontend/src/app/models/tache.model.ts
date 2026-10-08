export type StatutApprobation = 'EN_ATTENTE' | 'APPROUVEE' | 'REJETEE';

export interface Tache {
    id: number;
    stageId: number;
    date: string;
    titre: string;
    description: string;
    pieceJointe?: string;
    statutApprobation: StatutApprobation;
    commentaireEncadrant?: string;
    dateSaisie: string;
    dateApprobation?: string;
}

export interface Progression {
    nombreJoursTotal: number;
    nombreJoursCouverts: number;
    pourcentage: number;
}
