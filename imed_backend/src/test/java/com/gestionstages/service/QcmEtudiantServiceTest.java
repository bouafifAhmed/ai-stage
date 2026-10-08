package com.gestionstages.service;

import com.gestionstages.dto.QcmDTOs.ReponseQuestionRequest;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmRequest;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmResponse;
import com.gestionstages.model.OptionReponse;
import com.gestionstages.model.Qcm;
import com.gestionstages.model.Question;
import com.gestionstages.model.Role;
import com.gestionstages.model.TypeQuestion;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.OptionReponseRepository;
import com.gestionstages.repository.QcmRepository;
import com.gestionstages.repository.QuestionRepository;
import com.gestionstages.repository.TentativeQcmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QcmEtudiantServiceTest {

    private QcmRepository qcmRepository;
    private QuestionRepository questionRepository;
    private OptionReponseRepository optionReponseRepository;
    private TentativeQcmRepository tentativeQcmRepository;
    private QcmEtudiantService service;

    @BeforeEach
    void setUp() {
        qcmRepository = mock(QcmRepository.class);
        questionRepository = mock(QuestionRepository.class);
        optionReponseRepository = mock(OptionReponseRepository.class);
        tentativeQcmRepository = mock(TentativeQcmRepository.class);
        service = new QcmEtudiantService(
                qcmRepository,
                questionRepository,
                optionReponseRepository,
                tentativeQcmRepository
        );
    }

    @Test
    void valideUnQcmAvecToutesLesBonnesReponses() {
        Utilisateur etudiant = etudiant();
        Qcm qcm = qcm();
        Question question = question(1L, qcm);
        OptionReponse correcte = option(10L, question, true);
        OptionReponse incorrecte = option(11L, question, false);

        when(qcmRepository.findFirstByActifTrueOrderByIdAsc()).thenReturn(Optional.of(qcm));
        when(tentativeQcmRepository.existsByEtudiantIdAndQcmIdAndReussiTrue(2L, 1L)).thenReturn(false);
        when(tentativeQcmRepository.countByEtudiantIdAndQcmId(2L, 1L)).thenReturn(0L);
        when(questionRepository.findByQcmIdOrderByOrdreAsc(1L)).thenReturn(List.of(question));
        when(optionReponseRepository.findById(10L)).thenReturn(Optional.of(correcte));
        when(optionReponseRepository.existsByIdAndQuestionId(10L, 1L)).thenReturn(true);
        AtomicReference<com.gestionstages.model.TentativeQcm> tentativeSauvegardee = new AtomicReference<>();
        when(tentativeQcmRepository.save(any())).thenAnswer(invocation -> {
            tentativeSauvegardee.set(invocation.getArgument(0));
            return tentativeSauvegardee.get();
        });
        when(tentativeQcmRepository.findByEtudiantIdAndQcmIdOrderByDateSoumissionDesc(2L, 1L))
                .thenAnswer(invocation -> tentativeSauvegardee.get() == null
                        ? List.of()
                        : List.of(tentativeSauvegardee.get()));

        SoumettreQcmResponse response = service.soumettreAdmissibilite(
                etudiant,
                new SoumettreQcmRequest(List.of(new ReponseQuestionRequest(1L, 10L)))
        );

        assertEquals(100, response.scorePourcentage());
        assertTrue(response.reussi());
        assertTrue(response.admissible());
    }

    private Utilisateur etudiant() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(2L);
        utilisateur.setRole(Role.ETUDIANT);
        return utilisateur;
    }

    private Qcm qcm() {
        Qcm qcm = new Qcm();
        qcm.setId(1L);
        qcm.setTitre("QCM test");
        qcm.setNoteMinimalePassage(50);
        qcm.setNombreMaxTentatives(2);
        qcm.setActif(true);
        return qcm;
    }

    private Question question(Long id, Qcm qcm) {
        Question question = new Question();
        question.setId(id);
        question.setQcm(qcm);
        question.setPoints(1);
        question.setType(TypeQuestion.CHOIX_UNIQUE);
        return question;
    }

    private OptionReponse option(Long id, Question question, boolean correcte) {
        OptionReponse option = new OptionReponse();
        option.setId(id);
        option.setQuestion(question);
        option.setEstCorrecte(correcte);
        return option;
    }
}
