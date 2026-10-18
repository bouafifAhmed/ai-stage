package com.gestionstages.service;

import com.gestionstages.dto.*;
import com.gestionstages.model.*;
import com.gestionstages.repository.MessageReclamationRepository;
import com.gestionstages.repository.ReclamationRepository;
import com.gestionstages.repository.StageRepository;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReclamationServiceTest {
    private ReclamationRepository reclamations;
    private MessageReclamationRepository messages;
    private StageRepository stages;
    private UserRepository users;
    private ReclamationService service;

    private Utilisateur etudiant;
    private Utilisateur entreprise;
    private Candidature stage;

    @BeforeEach
    void setUp() {
        reclamations = mock(ReclamationRepository.class);
        messages = mock(MessageReclamationRepository.class);
        stages = mock(StageRepository.class);
        users = mock(UserRepository.class);
        service = new ReclamationService(reclamations, messages, stages, users);

        when(reclamations.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messages.save(any())).thenAnswer(inv -> inv.getArgument(0));

        etudiant = utilisateur(2L, Role.ETUDIANT, "Lina", "Doe");
        Entreprise acme = new Entreprise();
        acme.setId(10L);
        entreprise = utilisateur(3L, Role.ENTREPRISE, "Sami", "RH");
        entreprise.setEntreprise(acme);

        OffreStage offre = new OffreStage();
        offre.setId(20L);
        offre.setEntreprise(acme);
        stage = new Candidature();
        stage.setId(30L);
        stage.setEtudiant(etudiant);
        stage.setOffre(offre);

        when(users.findById(2L)).thenReturn(Optional.of(etudiant));
        when(users.findById(3L)).thenReturn(Optional.of(entreprise));
    }

    @Test
    void creeUneReclamationOuverteAvecSonPremierMessage() {
        when(stages.findByIdWithDetails(30L)).thenReturn(Optional.of(stage));

        ReclamationResponseDTO dto = service.creerReclamation(30L, demande(), 2L);

        assertEquals(StatutReclamation.OUVERTE, dto.getStatut());
        assertEquals("Lina Doe", dto.getNomEtudiant());
        ArgumentCaptor<MessageReclamation> message = ArgumentCaptor.forClass(MessageReclamation.class);
        verify(messages).save(message.capture());
        assertEquals("Ma note est fausse", message.getValue().getContenu());
        assertSame(etudiant, message.getValue().getAuteur());
    }

    @Test
    void refuseUneReclamationSurLeStageDUnAutreEtudiant() {
        Utilisateur autre = utilisateur(9L, Role.ETUDIANT, "Autre", "Etudiant");
        when(users.findById(9L)).thenReturn(Optional.of(autre));
        when(stages.findByIdWithDetails(30L)).thenReturn(Optional.of(stage));

        assertThrows(SecurityException.class, () -> service.creerReclamation(30L, demande(), 9L));
        verify(reclamations, never()).save(any());
    }

    @Test
    void refuseUneReclamationSurUnStageInexistant() {
        when(stages.findByIdWithDetails(30L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.creerReclamation(30L, demande(), 2L));
    }

    @Test
    void laPremiereReponseDeLEntrepriseLiéePasseEnTraitement() {
        Reclamation reclamation = reclamation(StatutReclamation.OUVERTE);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        MessageResponseDTO message = service.ajouterMessage(40L, message("Nous vérifions"), 3L, "ENTREPRISE");

        assertEquals(StatutReclamation.EN_TRAITEMENT, reclamation.getStatut());
        assertSame(entreprise, reclamation.getTraitePar());
        assertEquals("ENTREPRISE", message.getRoleAuteur());
        assertEquals("Sami RH", message.getNomAuteur());
    }

    @Test
    void refuseLeMessageDUneEntrepriseQuiNEstPasLiéeAuStage() {
        Entreprise autreSociete = new Entreprise();
        autreSociete.setId(99L);
        entreprise.setEntreprise(autreSociete);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation(StatutReclamation.OUVERTE)));

        assertThrows(SecurityException.class,
                () -> service.ajouterMessage(40L, message("Bonjour"), 3L, "ENTREPRISE"));
        verify(messages, never()).save(any());
    }

    @Test
    void refuseLeMessageDUnUtilisateurSansDroit() {
        Utilisateur intrus = utilisateur(9L, Role.ETUDIANT, "Autre", "Etudiant");
        when(users.findById(9L)).thenReturn(Optional.of(intrus));
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation(StatutReclamation.OUVERTE)));

        assertThrows(SecurityException.class,
                () -> service.ajouterMessage(40L, message("Bonjour"), 9L, "ETUDIANT"));
    }

    @Test
    void refuseUnMessageSurUneReclamationCloturee() {
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation(StatutReclamation.CLOTUREE)));

        assertThrows(IllegalArgumentException.class,
                () -> service.ajouterMessage(40L, message("Encore moi"), 2L, "ETUDIANT"));
        verify(messages, never()).save(any());
    }

    @Test
    void laContestationDeLEtudiantRemetUneReclamationResolueEnTraitement() {
        Reclamation reclamation = reclamation(StatutReclamation.RESOLUE);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        service.ajouterMessage(40L, message("Je ne suis pas d'accord"), 2L, "ETUDIANT");

        assertEquals(StatutReclamation.EN_TRAITEMENT, reclamation.getStatut());
    }

    @Test
    void unChefDeDepartementPeutPrendreEnChargeUneReclamation() {
        Utilisateur chef = utilisateur(5L, Role.CHEF_DEPT_STAGE, "Chef", "Stage");
        when(users.findById(5L)).thenReturn(Optional.of(chef));
        Reclamation reclamation = reclamation(StatutReclamation.OUVERTE);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        service.ajouterMessage(40L, message("Pris en charge"), 5L, "CHEF_DEPT_STAGE");

        assertEquals(StatutReclamation.EN_TRAITEMENT, reclamation.getStatut());
        assertSame(chef, reclamation.getTraitePar());
    }

    @Test
    void seulLeTraiteurPeutResoudre() {
        Reclamation reclamation = reclamation(StatutReclamation.EN_TRAITEMENT);
        reclamation.setTraitePar(entreprise);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        assertThrows(SecurityException.class, () -> service.resoudreReclamation(40L, 2L));

        ReclamationResponseDTO dto = service.resoudreReclamation(40L, 3L);
        assertEquals(StatutReclamation.RESOLUE, dto.getStatut());
        assertEquals("Sami RH", dto.getNomTraitePar());
    }

    @Test
    void refuseDeResoudreUneReclamationCloturee() {
        Reclamation reclamation = reclamation(StatutReclamation.CLOTUREE);
        reclamation.setTraitePar(entreprise);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        assertThrows(IllegalArgumentException.class, () -> service.resoudreReclamation(40L, 3L));
    }

    @Test
    void laClotureFixeLaDateDeCloture() {
        Reclamation reclamation = reclamation(StatutReclamation.RESOLUE);
        reclamation.setTraitePar(entreprise);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        ReclamationResponseDTO dto = service.cloturerReclamation(40L, 3L);

        assertEquals(StatutReclamation.CLOTUREE, dto.getStatut());
        assertNotNull(dto.getDateCloture());
        assertThrows(SecurityException.class, () -> service.cloturerReclamation(40L, 2L));
    }

    @Test
    void lEtudiantPeutRouvrirDansLesSeptJours() {
        Reclamation reclamation = reclamation(StatutReclamation.CLOTUREE);
        reclamation.setDateCloture(LocalDateTime.now().minusDays(6));
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        ReclamationResponseDTO dto = service.rouvrirReclamation(40L, 2L);

        assertEquals(StatutReclamation.EN_TRAITEMENT, dto.getStatut());
        assertNull(dto.getDateCloture());
    }

    @Test
    void refuseLaReouvertureApresSeptJours() {
        Reclamation reclamation = reclamation(StatutReclamation.CLOTUREE);
        reclamation.setDateCloture(LocalDateTime.now().minusDays(8));
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service.rouvrirReclamation(40L, 2L));
        assertTrue(erreur.getMessage().contains("7 jours"));
    }

    @Test
    void refuseLaReouvertureDUneReclamationNonClotureeOuDUnAutreEtudiant() {
        Reclamation ouverte = reclamation(StatutReclamation.EN_TRAITEMENT);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(ouverte));

        assertThrows(IllegalArgumentException.class, () -> service.rouvrirReclamation(40L, 2L));
        assertThrows(SecurityException.class, () -> service.rouvrirReclamation(40L, 9L));
    }

    @Test
    void clotureAutomatiquementLesReclamationsResoluesSansReponseDepuisCinqJours() {
        Reclamation ancienne = reclamation(StatutReclamation.RESOLUE);
        ancienne.setId(41L);
        Reclamation recente = reclamation(StatutReclamation.RESOLUE);
        recente.setId(42L);
        Reclamation enCours = reclamation(StatutReclamation.EN_TRAITEMENT);
        enCours.setId(43L);
        when(reclamations.findAll()).thenReturn(List.of(ancienne, recente, enCours));
        when(messages.findByReclamationIdOrderByDateEnvoiAsc(41L))
                .thenReturn(List.of(messageDate(LocalDateTime.now().minusDays(10)), messageDate(LocalDateTime.now().minusDays(6))));
        when(messages.findByReclamationIdOrderByDateEnvoiAsc(42L))
                .thenReturn(List.of(messageDate(LocalDateTime.now().minusDays(1))));

        service.clotureAutomatique();

        assertEquals(StatutReclamation.CLOTUREE, ancienne.getStatut());
        assertNotNull(ancienne.getDateCloture());
        assertEquals(StatutReclamation.RESOLUE, recente.getStatut());
        assertEquals(StatutReclamation.EN_TRAITEMENT, enCours.getStatut());
        verify(messages, never()).findByReclamationIdOrderByDateEnvoiAsc(43L);
    }

    @Test
    void leDetailContientLesMessagesDansLOrdre() {
        Reclamation reclamation = reclamation(StatutReclamation.EN_TRAITEMENT);
        when(reclamations.findByIdWithDetails(40L)).thenReturn(Optional.of(reclamation));
        MessageReclamation premier = messageDate(LocalDateTime.now().minusDays(2));
        premier.setAuteur(etudiant);
        premier.setContenu("Bonjour");
        MessageReclamation sansAuteur = messageDate(LocalDateTime.now());
        when(messages.findByReclamationIdOrderByDateEnvoiAsc(40L)).thenReturn(List.of(premier, sansAuteur));

        ReclamationDetailDTO detail = service.getDetailReclamation(40L);

        assertEquals(2, detail.getMessages().size());
        assertEquals("ETUDIANT", detail.getMessages().get(0).getRoleAuteur());
        assertEquals("N/A", detail.getMessages().get(1).getNomAuteur());
        assertEquals("UNKNOWN", detail.getMessages().get(1).getRoleAuteur());
    }

    @Test
    void listeLesReclamationsParStageEtParEntreprise() {
        when(reclamations.findByStageId(30L)).thenReturn(List.of(reclamation(StatutReclamation.OUVERTE)));
        when(reclamations.findByEntrepriseId(10L)).thenReturn(List.of());

        assertEquals(1, service.listerReclamationsParStage(30L).size());
        assertTrue(service.listerReclamationsParEntreprise(10L).isEmpty());
    }

    private Reclamation reclamation(StatutReclamation statut) {
        Reclamation r = new Reclamation();
        r.setId(40L);
        r.setStage(stage);
        r.setEtudiant(etudiant);
        r.setTypeReclamation(TypeReclamation.NOTE);
        r.setObjet("Note de stage");
        r.setStatut(statut);
        return r;
    }

    private MessageReclamation messageDate(LocalDateTime date) {
        MessageReclamation m = new MessageReclamation();
        m.setDateEnvoi(date);
        return m;
    }

    private CreateReclamationDTO demande() {
        CreateReclamationDTO dto = new CreateReclamationDTO();
        dto.setTypeReclamation(TypeReclamation.NOTE);
        dto.setObjet("Note de stage");
        dto.setMessageInitial("Ma note est fausse");
        return dto;
    }

    private CreateMessageDTO message(String contenu) {
        CreateMessageDTO dto = new CreateMessageDTO();
        dto.setContenu(contenu);
        return dto;
    }

    private Utilisateur utilisateur(Long id, Role role, String prenom, String nom) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setRole(role);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setEmail(prenom.toLowerCase() + "@example.com");
        return u;
    }
}
