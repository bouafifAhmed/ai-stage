package com.gestionstages.service;

import com.gestionstages.dto.QcmDTOs.QcmExamenResponse;
import com.gestionstages.dto.QcmDTOs.QcmStatutResponse;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.OptionReponseRepository;
import com.gestionstages.repository.QcmRepository;
import com.gestionstages.repository.QuestionRepository;
import com.gestionstages.repository.TentativeQcmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Accès au QCM d'admissibilité, statut des tentatives et blocage des candidatures.
 */
class QcmAdmissibiliteTest {
    private QcmRepository qcmRepository;
    private QuestionRepository questionRepository;
    private OptionReponseRepository optionRepository;
    private TentativeQcmRepository tentativeRepository;
    private QcmEtudiantService service;
    private Utilisateur etudiant;
    private Qcm qcm;

    @BeforeEach
    void setUp() {
        qcmRepository = mock(QcmRepository.class);
        questionRepository = mock(QuestionRepository.class);
        optionRepository = mock(OptionReponseRepository.class);
        tentativeRepository = mock(TentativeQcmRepository.class);
        service = new QcmEtudiantService(qcmRepository, questionRepository, optionRepository, tentativeRepository);

        etudiant = new Utilisateur();
        etudiant.setId(2L);
        etudiant.setRole(Role.ETUDIANT);
        qcm = new Qcm();
        qcm.setId(1L);
        qcm.setTitre("QCM d'admissibilité");
        qcm.setNoteMinimalePassage(50);
        qcm.setNombreMaxTentatives(2);
        qcm.setActif(true);
        when(qcmRepository.findFirstByActifTrueOrderByIdAsc()).thenReturn(Optional.of(qcm));
    }

    @Test
    void lExamenNeDevoilePasLesBonnesReponses() {
        Question question = new Question();
        question.setId(5L);
        question.setEnonce("2 + 2 ?");
        question.setType(TypeQuestion.CHOIX_UNIQUE);
        question.setPoints(1);
        OptionReponse bonne = new OptionReponse();
        bonne.setId(7L);
        bonne.setLibelle("4");
        bonne.setEstCorrecte(true);
        when(questionRepository.findByQcmIdOrderByOrdreAsc(1L)).thenReturn(List.of(question));
        when(optionRepository.findByQuestionIdOrderByOrdreAsc(5L)).thenReturn(List.of(bonne));

        QcmExamenResponse examen = service.obtenirQcmAdmissibilite(etudiant);

        assertEquals(1, examen.questions().size());
        assertEquals("4", examen.questions().get(0).options().get(0).libelle());
        assertFalse(examen.toString().contains("estCorrecte"), "Le DTO d'examen ne doit pas exposer la bonne réponse");
    }

    @Test
    void lExamenEstRefuseApresUneReussiteOuQuandLesTentativesSontEpuisees() {
        when(tentativeRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(2L, 1L)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(StageBusinessException.class,
                () -> service.obtenirQcmAdmissibilite(etudiant)).getStatus());

        when(tentativeRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(2L, 1L)).thenReturn(false);
        when(tentativeRepository.countByEtudiantIdAndQcmId(2L, 1L)).thenReturn(2L);
        StageBusinessException epuise = assertThrows(StageBusinessException.class,
                () -> service.obtenirQcmAdmissibilite(etudiant));
        assertTrue(epuise.getMessage().contains("tentatives"));
    }

    @Test
    void leStatutCompteLesTentativesEtGardeLeMeilleurScore() {
        when(tentativeRepository.findByEtudiantIdAndQcmIdOrderByDateSoumissionDesc(2L, 1L))
                .thenReturn(List.of(tentative(40, false)));

        QcmStatutResponse statut = service.statutAdmissibilite(etudiant);

        assertFalse(statut.admissible());
        assertEquals(1, statut.tentativesUtilisees());
        assertEquals(1, statut.tentativesRestantes());
        assertEquals(40, statut.meilleurScore());
    }

    @Test
    void leStatutEstAdmissibleDesQuUneTentativeEstReussie() {
        when(tentativeRepository.findByEtudiantIdAndQcmIdOrderByDateSoumissionDesc(2L, 1L))
                .thenReturn(List.of(tentative(80, true), tentative(30, false), tentative(20, false)));

        QcmStatutResponse statut = service.statutAdmissibilite(etudiant);

        assertTrue(statut.admissible());
        assertEquals(0, statut.tentativesRestantes());
        assertEquals(80, statut.meilleurScore());
    }

    @Test
    void laCandidatureExigeUnQcmReussi() {
        when(tentativeRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(2L, 1L)).thenReturn(false);
        assertEquals(HttpStatus.CONFLICT, assertThrows(StageBusinessException.class,
                () -> service.verifierAdmissibilite(etudiant)).getStatus());

        when(tentativeRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(2L, 1L)).thenReturn(true);
        assertDoesNotThrow(() -> service.verifierAdmissibilite(etudiant));
    }

    @Test
    void sansQcmActifOuPourUnAutreRoleLAccesEstRefuse() {
        Utilisateur entreprise = new Utilisateur();
        entreprise.setRole(Role.ENTREPRISE);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(StageBusinessException.class,
                () -> service.statutAdmissibilite(entreprise)).getStatus());

        when(qcmRepository.findFirstByActifTrueOrderByIdAsc()).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(StageBusinessException.class,
                () -> service.statutAdmissibilite(etudiant)).getStatus());
    }

    private TentativeQcm tentative(int score, boolean reussi) {
        TentativeQcm tentative = new TentativeQcm();
        tentative.setScorePourcentage(score);
        tentative.setReussi(reussi);
        return tentative;
    }
}
