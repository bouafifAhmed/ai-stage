export interface ProfilEtudiant {
  id?: number;
  nom?: string;
  prenom?: string;
  email?: string;
  telephone: string;
  filiere: string;
  niveauEtudes: string;
  competences: string[];
  cvPresent: boolean;
  cvNomFichier?: string;
  cvDateDepot?: string;
}

export interface ProfilEtudiantRequest {
  telephone: string;
  filiere: string;
  niveauEtudes: string;
  competences: string[];
}

export interface CvExtraction {
  telephone?: string;
  filiere?: string;
  niveauEtudes?: string;
  competences: string[];
  texteDetecte: boolean;
}

export interface CvUploadResult {
  profil: ProfilEtudiant;
  extraction: CvExtraction;
}
