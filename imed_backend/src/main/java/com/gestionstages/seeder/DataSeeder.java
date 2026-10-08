package com.gestionstages.seeder;

import com.gestionstages.model.OptionReponse;
import com.gestionstages.model.Qcm;
import com.gestionstages.model.Question;
import com.gestionstages.model.TypeQuestion;
import com.gestionstages.repository.OptionReponseRepository;
import com.gestionstages.repository.QcmRepository;
import com.gestionstages.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    private final QcmRepository qcmRepository;
    private final QuestionRepository questionRepository;
    private final OptionReponseRepository optionReponseRepository;

    public DataSeeder(
            QcmRepository qcmRepository,
            QuestionRepository questionRepository,
            OptionReponseRepository optionReponseRepository
    ) {
        this.qcmRepository = qcmRepository;
        this.questionRepository = questionRepository;
        this.optionReponseRepository = optionReponseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (qcmRepository.count() > 0) {
            LOGGER.info("Seeding QCM ignoré : {} QCM déjà présent(s) en base", qcmRepository.count());
            return;
        }

        Qcm qcm = creerQcmEntretienSavoirEtre();
        qcmRepository.save(qcm);
        int nombreQuestions = insererQuestionsEntretienSavoirEtre(qcm);

        LOGGER.info(
                "Seeding QCM effectué : « {} » ({} questions insérées)",
                qcm.getTitre(),
                nombreQuestions
        );
    }

    private Qcm creerQcmEntretienSavoirEtre() {
        Qcm qcm = new Qcm();
        qcm.setTitre("QCM Admissibilité - Entretien Comportemental");
        qcm.setDomaine("Savoir-être");
        qcm.setDureeMinutes(15);
        qcm.setNoteMinimalePassage(50);
        qcm.setNombreMaxTentatives(2);
        qcm.setActif(true);
        return qcm;
    }

    private int insererQuestionsEntretienSavoirEtre(Qcm qcm) {
        List<QuestionSeed> questions = List.of(
                new QuestionSeed(
                        """
                                Vous êtes en retard pour une réunion importante avec votre encadrant. \
                                Que faites-vous ?""",
                        List.of(
                                "Vous arrivez sans rien dire pour ne pas perdre plus de temps",
                                "Vous prévenez à l'avance et vous excusez à votre arrivée",
                                "Vous évitez la réunion et vous justifiez plus tard",
                                "Vous demandez à un collègue de vous excuser à votre place"
                        ),
                        1
                ),
                new QuestionSeed(
                        """
                                Un collègue commet une erreur qui impacte votre travail. Quelle est la \
                                meilleure attitude à adopter ?""",
                        List.of(
                                "Le signaler publiquement pour que ça ne se reproduise pas",
                                "Ignorer la situation pour éviter les conflits",
                                "En discuter calmement avec lui en privé pour trouver une solution",
                                "Corriger l'erreur sans en parler à personne"
                        ),
                        2
                ),
                new QuestionSeed(
                        """
                                Quelle qualité est généralement considérée comme la plus importante en \
                                milieu professionnel ?""",
                        List.of(
                                "Travailler le plus rapidement possible, quitte à faire des erreurs",
                                "La capacité à communiquer clairement et à collaborer",
                                "Ne jamais poser de questions pour paraître autonome",
                                "Toujours être d'accord avec sa hiérarchie"
                        ),
                        1
                ),
                new QuestionSeed(
                        """
                                Face à une critique constructive de votre encadrant, la meilleure \
                                attitude est de :""",
                        List.of(
                                "Se justifier immédiatement pour défendre son travail",
                                "L'écouter, en tenir compte et l'utiliser pour s'améliorer",
                                "L'ignorer si elle ne semble pas fondée",
                                "Prendre la critique personnellement et se décourager"
                        ),
                        1
                ),
                new QuestionSeed(
                        """
                                Vous devez respecter une deadline serrée sur une tâche complexe. Que \
                                privilégiez-vous ?""",
                        List.of(
                                "Rendre un travail incomplet mais dans les temps, sans prévenir",
                                "Travailler seul sans en parler pour ne pas déranger l'équipe",
                                """
                                        Communiquer rapidement sur les difficultés rencontrées et demander \
                                        de l'aide si nécessaire""",
                                "Repousser la deadline sans en informer personne"
                        ),
                        2
                ),
                new QuestionSeed(
                        """
                                Comment gérez-vous un désaccord avec un membre de votre équipe sur la \
                                façon de réaliser une tâche ?""",
                        List.of(
                                "Vous imposez votre point de vue car vous êtes sûr d'avoir raison",
                                """
                                        Vous exposez calmement votre point de vue et écoutez le sien pour \
                                        trouver un compromis""",
                                "Vous évitez le sujet pour ne pas créer de tension",
                                "Vous en parlez à d'autres collègues pour avoir du soutien"
                        ),
                        1
                ),
                new QuestionSeed(
                        "Qu'est-ce qui définit le mieux un bon esprit d'équipe ?",
                        List.of(
                                "Faire uniquement sa propre part de travail sans se soucier du reste",
                                """
                                        Aider les autres et partager les informations utiles à l'objectif \
                                        commun""",
                                "Prendre toutes les décisions à la place du groupe",
                                "Éviter de communiquer pour rester concentré sur sa tâche"
                        ),
                        1
                ),
                new QuestionSeed(
                        """
                                Vous devez réaliser une tâche pour laquelle vous manquez de compétences. \
                                Quelle est la meilleure démarche ?""",
                        List.of(
                                "Prétendre savoir le faire pour ne pas paraître incompétent",
                                "Refuser la tâche sans explication",
                                """
                                        Demander de l'aide ou de la formation, et être transparent sur vos \
                                        limites""",
                                "La confier à quelqu'un d'autre sans en informer votre responsable"
                        ),
                        2
                ),
                new QuestionSeed(
                        "Quelle est la meilleure définition du professionnalisme en entreprise ?",
                        List.of(
                                "Ne jamais montrer ses émotions",
                                "Respecter ses engagements, être ponctuel et honnête dans son travail",
                                "Toujours dire oui à toutes les demandes",
                                "Travailler le plus d'heures possible"
                        ),
                        1
                ),
                new QuestionSeed(
                        "Pourquoi la ponctualité est-elle importante en entreprise ?",
                        List.of(
                                "Uniquement pour éviter des sanctions",
                                """
                                        Elle montre du respect envers les autres et une bonne organisation \
                                        personnelle""",
                                "Elle n'a pas vraiment d'importance si le travail est bien fait",
                                "Elle est seulement utile pour les réunions formelles"
                        ),
                        1
                )
        );

        for (int index = 0; index < questions.size(); index++) {
            QuestionSeed seed = questions.get(index);
            creerQuestion(qcm, seed.enonce(), index + 1, seed.options(), seed.indexCorrecte());
        }
        return questions.size();
    }

    private void creerQuestion(
            Qcm qcm,
            String enonce,
            int ordre,
            List<String> options,
            int indexCorrecte
    ) {
        Question question = new Question();
        question.setQcm(qcm);
        question.setEnonce(enonce.trim());
        question.setType(TypeQuestion.CHOIX_UNIQUE);
        question.setPoints(1);
        question.setOrdre(ordre);
        question = questionRepository.save(question);

        for (int index = 0; index < options.size(); index++) {
            OptionReponse option = new OptionReponse();
            option.setQuestion(question);
            option.setLibelle(options.get(index));
            option.setEstCorrecte(index == indexCorrecte);
            option.setOrdre(index + 1);
            optionReponseRepository.save(option);
        }
    }

    private record QuestionSeed(String enonce, List<String> options, int indexCorrecte) {
    }
}
