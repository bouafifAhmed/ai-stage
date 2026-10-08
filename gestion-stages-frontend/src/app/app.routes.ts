import { Routes } from '@angular/router';

import { AccessDeniedComponent } from './components/access-denied/access-denied.component';
import { AssignerTacheComponent } from './components/assigner-tache/assigner-tache.component';
import { CalendrierEcheancesComponent } from './components/calendrier-echeances/calendrier-echeances.component';
import { CandidatureDetailComponent } from './components/candidature-detail/candidature-detail.component';
import { ClotureStageComponent } from './components/cloture-stage/cloture-stage.component';
import { EntrepriseAdminListComponent } from './components/entreprise-admin-list/entreprise-admin-list.component';
import { EtudiantAdminListComponent } from './components/etudiant-admin-list/etudiant-admin-list.component';
import { EntrepriseOffresComponent } from './components/entreprise-offres/entreprise-offres.component';
import { EntrepriseReclamationsComponent } from './components/entreprise-reclamations/entreprise-reclamations.component';
import { EtudiantOffreDetailComponent } from './components/etudiant-offre-detail/etudiant-offre-detail.component';
import { EtudiantOffresComponent } from './components/etudiant-offres/etudiant-offres.component';
import { JournalStageComponent } from './components/journal-stage/journal-stage.component';
import { LoginComponent } from './components/login/login.component';
import { MesCandidaturesComponent } from './components/mes-candidatures/mes-candidatures.component';
import { MesTachesAssigneesComponent } from './components/mes-taches-assignees/mes-taches-assignees.component';
import { NotificationsEtudiantComponent } from './components/notifications-etudiant/notifications-etudiant.component';
import { OffreCandidaturesComponent } from './components/offre-candidatures/offre-candidatures.component';
import { OffreFormComponent } from './components/offre-form/offre-form.component';
import { ProfilEtudiantComponent } from './components/profil-etudiant/profil-etudiant.component';
import { RegisterComponent } from './components/register/register.component';
import { ValidationTachesAssigneesComponent } from './components/validation-taches-assignees/validation-taches-assignees.component';
import { WorkspaceComponent } from './components/workspace/workspace.component';
import { ReclamationListComponent } from './components/reclamation-list/reclamation-list.component';
import { ReclamationDetailComponent } from './components/reclamation-detail/reclamation-detail.component';
import { RecommandationsComponent } from './components/recommandations/recommandations.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'espace-etudiant',
    canActivate: [authGuard, roleGuard(['ETUDIANT'])],
    children: [
      { path: '', component: EtudiantOffresComponent },
      { path: 'offres/:id', component: EtudiantOffreDetailComponent },
      { path: 'profil', component: ProfilEtudiantComponent },
      { path: 'candidatures', component: MesCandidaturesComponent },
      { path: 'recommandations', component: RecommandationsComponent },
      { path: 'notifications', component: NotificationsEtudiantComponent },
      { path: 'reclamations', component: ReclamationListComponent },
      { path: 'reclamations/:id', component: ReclamationDetailComponent },
      { path: 'calendrier', component: CalendrierEcheancesComponent, data: { role: 'ETUDIANT' } },
      { path: 'stages/:stageId/taches-assignees', component: MesTachesAssigneesComponent },
      { path: 'journal/:stageId', component: JournalStageComponent },
      { path: 'stages/:stageId/journal', component: JournalStageComponent },
      { path: 'stages/:stageId/calendrier', component: CalendrierEcheancesComponent, data: { role: 'ETUDIANT' } },
      { path: 'stages/:stageId/cloture', component: ClotureStageComponent, data: { role: 'ETUDIANT' } },
    ],
  },
  {
    path: 'espace-entreprise',
    canActivate: [authGuard, roleGuard(['ENTREPRISE'])],
    children: [
      { path: '', component: EntrepriseOffresComponent },
      { path: 'reclamations', component: EntrepriseReclamationsComponent },
      { path: 'reclamations/:id', component: ReclamationDetailComponent },
      { path: 'offres/nouvelle', component: OffreFormComponent },
      { path: 'offres/:id/modifier', component: OffreFormComponent },
      { path: 'offres/:id/candidatures', component: OffreCandidaturesComponent },
      { path: 'candidatures/:id', component: CandidatureDetailComponent },
      { path: 'stages/:stageId/taches-assignees/assigner', component: AssignerTacheComponent },
      { path: 'stages/:stageId/taches-assignees/validation', component: ValidationTachesAssigneesComponent },
      { path: 'stages/:stageId/calendrier', component: CalendrierEcheancesComponent, data: { role: 'ENTREPRISE' } },
      { path: 'stages/:stageId/cloture', component: ClotureStageComponent, data: { role: 'ENTREPRISE' } },
    ],
  },
  {
    path: 'espace-administration',
    component: WorkspaceComponent,
    canActivate: [
      authGuard,
      roleGuard([
        'CHEF_DEPT_STAGE',
        'CHEF_DEPT_PEDAGOGIQUE',
        'SUPER_ADMIN',
      ]),
    ],
    data: { title: 'Espace administration' },
  },
  {
    path: 'espace-super-admin',
    component: WorkspaceComponent,
    canActivate: [authGuard, roleGuard(['SUPER_ADMIN'])],
    data: { title: 'Espace super administrateur' },
  },
  {
    path: 'admin/entreprises',
    component: EntrepriseAdminListComponent,
    canActivate: [authGuard, roleGuard(['SUPER_ADMIN'])],
  },
  {
    path: 'admin/etudiants',
    component: EtudiantAdminListComponent,
    canActivate: [authGuard, roleGuard(['SUPER_ADMIN'])],
  },
  {
    path: 'interdit',
    component: AccessDeniedComponent,
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'login' },
];
