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

import com.gestionstages.model.Entreprise;
import com.gestionstages.model.OffreStage;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.OffreStageRepository;
import com.gestionstages.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    private final QcmRepository qcmRepository;
    private final QuestionRepository questionRepository;
    private final OptionReponseRepository optionReponseRepository;
    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final OffreStageRepository offreStageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            QcmRepository qcmRepository,
            QuestionRepository questionRepository,
            OptionReponseRepository optionReponseRepository,
            UserRepository userRepository,
            EntrepriseRepository entrepriseRepository,
            OffreStageRepository offreStageRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.qcmRepository = qcmRepository;
        this.questionRepository = questionRepository;
        this.optionReponseRepository = optionReponseRepository;
        this.userRepository = userRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.offreStageRepository = offreStageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seederComptesEtOffres();

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

    private void seederComptesEtOffres() {
        if (userRepository.existsByEmail("etudiant@demo.com")) {
            LOGGER.info("Seeding étudiants et offres ignoré : le compte étudiant existe déjà.");
            return;
        }

        LOGGER.info("Initialisation des comptes de démonstration et des offres de stage...");

        // 1. Admin
        if (!userRepository.existsByEmail("admin@demo.com")) {
            Utilisateur admin = new Utilisateur();
            admin.setNom("System");
            admin.setPrenom("Admin");
            admin.setEmail("admin@demo.com");
            admin.setMotDePasse(passwordEncoder.encode("Password123!"));
            admin.setRole(Role.SUPER_ADMIN);
            admin.setActif(true);
            userRepository.save(admin);
        }

        // 2. Entreprise
        Entreprise techCorp = new Entreprise();
        techCorp.setNom("TechCorp Innovations");
        techCorp.setEmailContact("recruteur@techcorp.com");
        techCorp.setStatutValidation(StatutValidation.VALIDEE);
        techCorp.setSiteWeb("https://techcorp-demo.com");
        techCorp.setAdresse("Paris, France");
        techCorp.setSecteurActivite("Technologies & Software");
        techCorp = entrepriseRepository.save(techCorp);

        Utilisateur recruteur = new Utilisateur();
        recruteur.setNom("TechCorp");
        recruteur.setPrenom("Recruteur");
        recruteur.setEmail("recruteur@techcorp.com");
        recruteur.setMotDePasse(passwordEncoder.encode("Password123!"));
        recruteur.setRole(Role.ENTREPRISE);
        recruteur.setEntreprise(techCorp);
        recruteur.setActif(true);
        userRepository.save(recruteur);

        // 3. Etudiant
        Utilisateur etudiant = new Utilisateur();
        etudiant.setNom("Dupont");
        etudiant.setPrenom("Alexandre");
        etudiant.setEmail("etudiant@demo.com");
        etudiant.setMotDePasse(passwordEncoder.encode("Password123!"));
        etudiant.setRole(Role.ETUDIANT);
        etudiant.setFiliere("Génie Logiciel & Cloud");
        etudiant.setNiveauEtudes("Bac+5");
        etudiant.setTelephone("0612345678");
        etudiant.setCompetences(new ArrayList<>(List.of("Java", "Spring Boot", "Angular", "TypeScript", "Docker", "Git")));
        etudiant.setActif(true);
        userRepository.save(etudiant);

        // 4. Offres de stage
        OffreStage offre1 = new OffreStage();
        offre1.setTitre("Stage Développeur Fullstack Java / Angular");
        offre1.setDescription("Rejoignez notre équipe agile pour concevoir et développer des fonctionnalités complètes sur nos plateformes web et cloud microservices.");
        offre1.setDomaine("Génie Logiciel");
        offre1.setLocalisation("Paris / Télétravail partiel");
        offre1.setMode(OffreStage.ModeTravail.HYBRIDE);
        offre1.setTypeStage("PFE / Fin d'études");
        offre1.setDureeMois(6);
        offre1.setDateDebut(LocalDate.now().plusWeeks(2));
        offre1.setDateFin(LocalDate.now().plusMonths(6));
        offre1.setDateLimite(LocalDate.now().plusMonths(2));
        offre1.setCompetences(new ArrayList<>(List.of("Java", "Spring Boot", "Angular", "PostgreSQL", "Docker")));
        offre1.setNiveauRequis("Bac+5");
        offre1.setNombrePlaces(3);
        offre1.setRemuneration(BigDecimal.valueOf(1400.00));
        offre1.setStatut(OffreStage.StatutOffre.PUBLIEE);
        offre1.setEntreprise(techCorp);
        offreStageRepository.save(offre1);

        OffreStage offre2 = new OffreStage();
        offre2.setTitre("Stage Ingénieur IA & NLP / Data Science");
        offre2.setDescription("Intégrez notre lab IA pour développer des modèles de traitement automatique du langage naturel, de recommandation et de machine learning avec FastAPI.");
        offre2.setDomaine("Intelligence Artificielle");
        offre2.setLocalisation("Lyon / Distanciel");
        offre2.setMode(OffreStage.ModeTravail.DISTANCIEL);
        offre2.setTypeStage("Stage R&D");
        offre2.setDureeMois(6);
        offre2.setDateDebut(LocalDate.now().plusWeeks(3));
        offre2.setDateFin(LocalDate.now().plusMonths(6));
        offre2.setDateLimite(LocalDate.now().plusMonths(2));
        offre2.setCompetences(new ArrayList<>(List.of("Python", "FastAPI", "Machine Learning", "NLP", "Scikit-Learn")));
        offre2.setNiveauRequis("Bac+5");
        offre2.setNombrePlaces(2);
        offre2.setRemuneration(BigDecimal.valueOf(1500.00));
        offre2.setStatut(OffreStage.StatutOffre.PUBLIEE);
        offre2.setEntreprise(techCorp);
        offreStageRepository.save(offre2);

        OffreStage offre3 = new OffreStage();
        offre3.setTitre("Stage Frontend UI/UX React & Next.js");
        offre3.setDescription("Développement d'interfaces modernes, interactives et performantes pour notre produit SaaS.");
        offre3.setDomaine("Web Frontend");
        offre3.setLocalisation("Nantes");
        offre3.setMode(OffreStage.ModeTravail.PRESENTIEL);
        offre3.setTypeStage("Stage Découverte");
        offre3.setDureeMois(4);
        offre3.setDateDebut(LocalDate.now().plusWeeks(4));
        offre3.setDateFin(LocalDate.now().plusMonths(5));
        offre3.setDateLimite(LocalDate.now().plusMonths(2));
        offre3.setCompetences(new ArrayList<>(List.of("React", "Next.js", "TailwindCSS", "TypeScript")));
        offre3.setNiveauRequis("Bac+3");
        offre3.setNombrePlaces(1);
        offre3.setRemuneration(BigDecimal.valueOf(1100.00));
        offre3.setStatut(OffreStage.StatutOffre.PUBLIEE);
        offre3.setEntreprise(techCorp);
        offreStageRepository.save(offre3);

        LOGGER.info("Seeding terminé avec succès : 3 comptes de test et 3 offres de stage créées.");
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
