package com.gestionstages.service;

import com.gestionstages.dto.*;
import com.gestionstages.model.*;
import com.gestionstages.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReclamationService {

    private static final Logger logger = LoggerFactory.getLogger(ReclamationService.class);

    private final ReclamationRepository reclamationRepository;
    private final MessageReclamationRepository messageRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;

    public ReclamationService(
            ReclamationRepository reclamationRepository,
            MessageReclamationRepository messageRepository,
            StageRepository stageRepository,
            UserRepository userRepository
    ) {
        this.reclamationRepository = reclamationRepository;
        this.messageRepository = messageRepository;
        this.stageRepository = stageRepository;
        this.userRepository = userRepository;
    }

    public ReclamationResponseDTO creerReclamation(Long stageId, CreateReclamationDTO dto, Long etudiantId) {
        // Charger le stage avec ses détails
        Candidature stage = stageRepository.findByIdWithDetails(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable"));

        // Charger l'étudiant
        Utilisateur etudiant = userRepository.findById(etudiantId)
                .orElseThrow(() -> new IllegalArgumentException("Étudiant introuvable"));

        // Vérifier que l'étudiant connecté est bien le propriétaire du stage
        if (stage.getEtudiant() == null || !stage.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Vous n'êtes pas le propriétaire de ce stage");
        }

        // Créer la réclamation
        Reclamation reclamation = new Reclamation();
        reclamation.setStage(stage);
        reclamation.setEtudiant(etudiant);
        reclamation.setTypeReclamation(dto.getTypeReclamation());
        reclamation.setObjet(dto.getObjet());
        reclamation.setStatut(StatutReclamation.OUVERTE);

        reclamation = reclamationRepository.save(reclamation);

        // Créer le premier message
        MessageReclamation message = new MessageReclamation();
        message.setReclamation(reclamation);
        message.setAuteur(etudiant);
        message.setContenu(dto.getMessageInitial());
        messageRepository.save(message);

        logger.info("Réclamation créée : ID {}, type {}, par étudiant ID {}", 
                    reclamation.getId(), dto.getTypeReclamation(), etudiantId);

        return mapToResponseDTO(reclamation);
    }

    public MessageResponseDTO ajouterMessage(Long reclamationId, CreateMessageDTO dto, Long auteurId, String roleAuteur) {
        // Charger la réclamation avec ses détails
        Reclamation reclamation = reclamationRepository.findByIdWithDetails(reclamationId)
                .orElseThrow(() -> new IllegalArgumentException("Réclamation introuvable"));

        // Charger l'auteur
        Utilisateur auteur = userRepository.findById(auteurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Vérifier les droits d'accès
        boolean isEtudiantProprietaire = reclamation.getEtudiant().getId().equals(auteurId);
        boolean isAdmin = "CHEF_DEPT_STAGE".equals(roleAuteur) || "CHEF_DEPT_PEDAGOGIQUE".equals(roleAuteur);
        boolean isEntreprise = "ENTREPRISE".equals(roleAuteur);

        // Vérifier que l'entreprise est bien celle liée au stage
        if (isEntreprise) {
            Utilisateur entrepriseUser = userRepository.findById(auteurId)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
            if (entrepriseUser.getEntreprise() == null ||
                !entrepriseUser.getEntreprise().getId().equals(
                        reclamation.getStage().getOffre().getEntreprise().getId())) {
                throw new SecurityException("Vous n'êtes pas l'entreprise liée à ce stage");
            }
        }

        if (!isEtudiantProprietaire && !isAdmin && !isEntreprise) {
            throw new SecurityException("Vous n'êtes pas autorisé à commenter cette réclamation");
        }

        // Vérifier que le statut permet d'ajouter un message
        if (reclamation.getStatut() == StatutReclamation.CLOTUREE) {
            throw new IllegalArgumentException("Impossible d'ajouter un message sur une réclamation clôturée. Utilisez la réouverture si vous êtes dans le délai.");
        }

        // Créer le message
        MessageReclamation message = new MessageReclamation();
        message.setReclamation(reclamation);
        message.setAuteur(auteur);
        message.setContenu(dto.getContenu());
        message.setPieceJointe(dto.getPieceJointe());
        message = messageRepository.save(message);

        // Gérer les transitions de statut
        if ((isAdmin || isEntreprise) && reclamation.getStatut() == StatutReclamation.OUVERTE) {
            // Un admin ou une entreprise répond pour la première fois
            reclamation.setStatut(StatutReclamation.EN_TRAITEMENT);
            reclamation.setTraitePar(auteur);
            reclamationRepository.save(reclamation);
            logger.info("Réclamation {} prise en charge par {} ({})", reclamationId, auteur.getNom(), roleAuteur);
        } else if (isEtudiantProprietaire && reclamation.getStatut() == StatutReclamation.RESOLUE) {
            // L'étudiant conteste une résolution = bouclage
            reclamation.setStatut(StatutReclamation.EN_TRAITEMENT);
            reclamationRepository.save(reclamation);
            logger.info("Réclamation {} rouverte par l'étudiant (contestation)", reclamationId);
        }

        return mapToMessageDTO(message);
    }

    public ReclamationResponseDTO resoudreReclamation(Long reclamationId, Long traiteurId) {
        Reclamation reclamation = reclamationRepository.findByIdWithDetails(reclamationId)
                .orElseThrow(() -> new IllegalArgumentException("Réclamation introuvable"));

        // Vérifier que le traiteur (admin ou entreprise) est bien celui qui traite la réclamation
        if (reclamation.getTraitePar() == null || !reclamation.getTraitePar().getId().equals(traiteurId)) {
            throw new SecurityException("Vous n'êtes pas en charge de cette réclamation");
        }

        if (reclamation.getStatut() == StatutReclamation.CLOTUREE) {
            throw new IllegalArgumentException("Impossible de résoudre une réclamation clôturée");
        }

        reclamation.setStatut(StatutReclamation.RESOLUE);
        reclamation = reclamationRepository.save(reclamation);

        logger.info("Réclamation {} marquée comme résolue par traiteur ID {}", reclamationId, traiteurId);

        return mapToResponseDTO(reclamation);
    }

    public ReclamationResponseDTO cloturerReclamation(Long reclamationId, Long traiteurId) {
        Reclamation reclamation = reclamationRepository.findByIdWithDetails(reclamationId)
                .orElseThrow(() -> new IllegalArgumentException("Réclamation introuvable"));

        // Vérifier que le traiteur (admin ou entreprise) est bien celui qui traite la réclamation
        if (reclamation.getTraitePar() == null || !reclamation.getTraitePar().getId().equals(traiteurId)) {
            throw new SecurityException("Vous n'êtes pas en charge de cette réclamation");
        }

        reclamation.setStatut(StatutReclamation.CLOTUREE);
        reclamation.setDateCloture(LocalDateTime.now());
        reclamation = reclamationRepository.save(reclamation);

        logger.info("Réclamation {} clôturée par traiteur ID {}", reclamationId, traiteurId);

        return mapToResponseDTO(reclamation);
    }

    public ReclamationResponseDTO rouvrirReclamation(Long reclamationId, Long etudiantId) {
        Reclamation reclamation = reclamationRepository.findByIdWithDetails(reclamationId)
                .orElseThrow(() -> new IllegalArgumentException("Réclamation introuvable"));

        // Vérifier que l'étudiant est bien l'auteur
        if (!reclamation.getEtudiant().getId().equals(etudiantId)) {
            throw new SecurityException("Vous n'êtes pas l'auteur de cette réclamation");
        }

        // Vérifier que le statut est CLOTUREE
        if (reclamation.getStatut() != StatutReclamation.CLOTUREE) {
            throw new IllegalArgumentException("Seule une réclamation clôturée peut être rouverte");
        }

        // Vérifier le délai de 7 jours
        if (reclamation.getDateCloture() == null) {
            throw new IllegalArgumentException("Date de clôture manquante");
        }

        LocalDateTime limiteReouverture = reclamation.getDateCloture().plusDays(7);
        if (LocalDateTime.now().isAfter(limiteReouverture)) {
            throw new IllegalArgumentException("Le délai de réouverture (7 jours) est dépassé");
        }

        reclamation.setStatut(StatutReclamation.REOUVERTE);
        // Passer immédiatement à EN_TRAITEMENT après réouverture
        reclamation.setStatut(StatutReclamation.EN_TRAITEMENT);
        reclamation.setDateCloture(null); // Réinitialiser la date de clôture
        reclamation = reclamationRepository.save(reclamation);

        logger.info("Réclamation {} rouverte par étudiant ID {} dans le délai", reclamationId, etudiantId);

        return mapToResponseDTO(reclamation);
    }

    /**
     * Clôture automatiquement les réclamations résolues depuis plus de 5 jours
     * sans nouveau message de l'étudiant
     */
    public void clotureAutomatique() {
        logger.info("Démarrage de la clôture automatique des réclamations");

        List<Reclamation> reclamations = reclamationRepository.findAll().stream()
                .filter(r -> r.getStatut() == StatutReclamation.RESOLUE)
                .collect(Collectors.toList());

        int count = 0;
        for (Reclamation reclamation : reclamations) {
            // Trouver le dernier message
            List<MessageReclamation> messages = messageRepository
                    .findByReclamationIdOrderByDateEnvoiAsc(reclamation.getId());

            if (!messages.isEmpty()) {
                MessageReclamation dernierMessage = messages.get(messages.size() - 1);
                LocalDateTime limite = dernierMessage.getDateEnvoi().plusDays(5);

                // Si le dernier message date de plus de 5 jours, clôturer
                if (LocalDateTime.now().isAfter(limite)) {
                    reclamation.setStatut(StatutReclamation.CLOTUREE);
                    reclamation.setDateCloture(LocalDateTime.now());
                    reclamationRepository.save(reclamation);
                    count++;
                    logger.info("Réclamation {} clôturée automatiquement", reclamation.getId());
                }
            }
        }

        logger.info("Clôture automatique terminée : {} réclamation(s) clôturée(s)", count);
    }

    public List<ReclamationResponseDTO> listerReclamationsParStage(Long stageId) {
        List<Reclamation> reclamations = reclamationRepository.findByStageId(stageId);
        return reclamations.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Liste toutes les réclamations des stagiaires liés à une entreprise
     */
    public List<ReclamationResponseDTO> listerReclamationsParEntreprise(Long entrepriseId) {
        List<Reclamation> reclamations = reclamationRepository.findByEntrepriseId(entrepriseId);
        return reclamations.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public ReclamationDetailDTO getDetailReclamation(Long reclamationId) {
        Reclamation reclamation = reclamationRepository.findByIdWithDetails(reclamationId)
                .orElseThrow(() -> new IllegalArgumentException("Réclamation introuvable"));

        List<MessageReclamation> messages = messageRepository
                .findByReclamationIdOrderByDateEnvoiAsc(reclamationId);

        ReclamationResponseDTO reclamationDTO = mapToResponseDTO(reclamation);
        List<MessageResponseDTO> messagesDTO = messages.stream()
                .map(this::mapToMessageDTO)
                .collect(Collectors.toList());

        return new ReclamationDetailDTO(reclamationDTO, messagesDTO);
    }

    // Méthodes de mapping
    private ReclamationResponseDTO mapToResponseDTO(Reclamation reclamation) {
        String nomEtudiant = "N/A";
        if (reclamation.getEtudiant() != null) {
            nomEtudiant = reclamation.getEtudiant().getPrenom() + " " + reclamation.getEtudiant().getNom();
        }

        String nomTraitePar = null;
        if (reclamation.getTraitePar() != null) {
            nomTraitePar = reclamation.getTraitePar().getPrenom() + " " + reclamation.getTraitePar().getNom();
        }

        return new ReclamationResponseDTO(
                reclamation.getId(),
                reclamation.getTypeReclamation(),
                reclamation.getObjet(),
                reclamation.getStatut(),
                reclamation.getDateCreation(),
                reclamation.getDateCloture(),
                nomEtudiant,
                nomTraitePar
        );
    }

    private MessageResponseDTO mapToMessageDTO(MessageReclamation message) {
        String nomAuteur = "N/A";
        String roleAuteur = "UNKNOWN";

        if (message.getAuteur() != null) {
            nomAuteur = message.getAuteur().getPrenom() + " " + message.getAuteur().getNom();
            roleAuteur = message.getAuteur().getRole() != null ? message.getAuteur().getRole().name() : "UNKNOWN";
        }

        return new MessageResponseDTO(
                message.getId(),
                message.getContenu(),
                message.getPieceJointe(),
                message.getDateEnvoi(),
                nomAuteur,
                roleAuteur
        );
    }
}
