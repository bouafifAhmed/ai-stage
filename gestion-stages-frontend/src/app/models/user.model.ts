export type Role =
  | 'ETUDIANT'
  | 'ENTREPRISE'
  | 'CHEF_DEPT_STAGE'
  | 'CHEF_DEPT_PEDAGOGIQUE'
  | 'SUPER_ADMIN';

export interface User {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  token: string | null;
  type: 'Bearer';
  user: User;
}
