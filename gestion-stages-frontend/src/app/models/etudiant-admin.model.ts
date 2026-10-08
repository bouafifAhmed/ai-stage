import { PageResponse } from './entreprise.model';

export interface EtudiantAdmin {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  filiere: string | null;
  niveauEtudes: string | null;
  competences: string[];
  cvPresent: boolean;
  cvNomFichier: string | null;
  cvDateDepot: string | null;
  actif: boolean;
  dateCreation: string;
}

export interface CreateEtudiantRequest {
  nom: string;
  prenom: string;
  email: string;
  motDePasse: string;
  telephone?: string;
  filiere?: string;
  niveauEtudes?: string;
  competences?: string[];
}

export type UpdateEtudiantRequest = Partial<
  Omit<CreateEtudiantRequest, 'motDePasse'>
>;

export type EtudiantPageResponse = PageResponse<EtudiantAdmin>;
