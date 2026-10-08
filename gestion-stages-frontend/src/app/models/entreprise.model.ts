export type TailleEntreprise = 'TPE' | 'PME' | 'ETI' | 'GRANDE_ENTREPRISE';
export type StatutValidation = 'EN_ATTENTE' | 'VALIDEE' | 'REJETEE';

export interface Entreprise {
  id: number;
  nom: string;
  adresse: string | null;
  ville: string | null;
  secteurActivite: string | null;
  taille: TailleEntreprise | null;
  emailContact: string;
  telephone: string | null;
  siteWeb: string | null;
  statutValidation: StatutValidation;
  dateInscription: string;
  motifRejet: string | null;
}

export interface CreateEntrepriseRequest {
  nom: string;
  adresse?: string;
  ville?: string;
  secteurActivite?: string;
  taille?: TailleEntreprise;
  emailContact: string;
  telephone?: string;
  siteWeb?: string;
  motDePasse: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
