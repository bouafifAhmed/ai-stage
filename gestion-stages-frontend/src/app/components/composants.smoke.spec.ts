import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AccessDeniedComponent } from './access-denied/access-denied.component';
import { AssignerTacheComponent } from './assigner-tache/assigner-tache.component';
import { CalendrierEcheancesComponent } from './calendrier-echeances/calendrier-echeances.component';
import { CandidatureDetailComponent } from './candidature-detail/candidature-detail.component';
import { ClotureStageComponent } from './cloture-stage/cloture-stage.component';
import { EntrepriseAdminListComponent } from './entreprise-admin-list/entreprise-admin-list.component';
import { EntrepriseFormComponent } from './entreprise-form/entreprise-form.component';
import { EntrepriseOffresComponent } from './entreprise-offres/entreprise-offres.component';
import { EntrepriseReclamationsComponent } from './entreprise-reclamations/entreprise-reclamations.component';
import { EtudiantAdminListComponent } from './etudiant-admin-list/etudiant-admin-list.component';
import { EtudiantFormComponent } from './etudiant-form/etudiant-form.component';
import { EtudiantOffreDetailComponent } from './etudiant-offre-detail/etudiant-offre-detail.component';
import { EtudiantOffresComponent } from './etudiant-offres/etudiant-offres.component';
import { JournalStageComponent } from './journal-stage/journal-stage.component';
import { LoginComponent } from './login/login.component';
import { MesCandidaturesComponent } from './mes-candidatures/mes-candidatures.component';
import { MesTachesAssigneesComponent } from './mes-taches-assignees/mes-taches-assignees.component';
import { NotificationsEtudiantComponent } from './notifications-etudiant/notifications-etudiant.component';
import { OffreCandidaturesComponent } from './offre-candidatures/offre-candidatures.component';
import { OffreFormComponent } from './offre-form/offre-form.component';
import { ProfilEtudiantComponent } from './profil-etudiant/profil-etudiant.component';
import { ProgressionBarComponent } from './progression-bar/progression-bar.component';
import { QcmPassageComponent } from './qcm-passage/qcm-passage.component';
import { ReclamationDetailComponent } from './reclamation-detail/reclamation-detail.component';
import { ReclamationFormComponent } from './reclamation-form/reclamation-form.component';
import { ReclamationListComponent } from './reclamation-list/reclamation-list.component';
import { ReclamationThreadComponent } from './reclamation-thread/reclamation-thread.component';
import { RegisterComponent } from './register/register.component';
import { TacheFormComponent } from './tache-form/tache-form.component';
import { ValidationTachesAssigneesComponent } from './validation-taches-assignees/validation-taches-assignees.component';
import { WorkspaceComponent } from './workspace/workspace.component';

/**
 * Chaque écran se crée et s'affiche sans erreur avec une route et un backend factices :
 * détecte les erreurs de template et les plantages au chargement.
 */
const COMPOSANTS: Type<unknown>[] = [
  AccessDeniedComponent, AssignerTacheComponent, CalendrierEcheancesComponent, CandidatureDetailComponent,
  ClotureStageComponent, EntrepriseAdminListComponent, EntrepriseFormComponent, EntrepriseOffresComponent,
  EntrepriseReclamationsComponent, EtudiantAdminListComponent, EtudiantFormComponent, EtudiantOffreDetailComponent,
  EtudiantOffresComponent, JournalStageComponent, LoginComponent, MesCandidaturesComponent,
  MesTachesAssigneesComponent, NotificationsEtudiantComponent, OffreCandidaturesComponent, OffreFormComponent,
  ProfilEtudiantComponent, ProgressionBarComponent, QcmPassageComponent, ReclamationDetailComponent,
  ReclamationFormComponent, ReclamationListComponent, ReclamationThreadComponent, RegisterComponent,
  TacheFormComponent, ValidationTachesAssigneesComponent, WorkspaceComponent,
];

const parametres = convertToParamMap({ id: '1', stageId: '1' });
const routeFactice = {
  snapshot: { paramMap: parametres, queryParamMap: convertToParamMap({}), data: { role: 'ETUDIANT', title: 'Espace' } },
  paramMap: of(parametres),
  queryParamMap: of(convertToParamMap({})),
  data: of({ role: 'ETUDIANT', title: 'Espace' }),
};

describe('Tous les écrans (smoke)', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: COMPOSANTS,
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: routeFactice },
      ],
    });
  });

  for (const composant of COMPOSANTS) {
    it(`crée et affiche ${composant.name}`, () => {
      const fixture = TestBed.createComponent(composant);
      fixture.detectChanges();

      expect(fixture.componentInstance).toBeTruthy();
      expect((fixture.nativeElement as HTMLElement).innerHTML.length).toBeGreaterThan(0);
    });
  }
});
