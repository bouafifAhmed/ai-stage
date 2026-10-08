package com.gestionstages.service;

import com.gestionstages.dto.CompleterTacheDTO;
import com.gestionstages.dto.CreateTacheAssigneeDTO;
import com.gestionstages.dto.TacheAssigneeResponseDTO;
import com.gestionstages.dto.ValiderTacheDTO;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.NotificationEtudiant;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutStage;
import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.TacheAssignee;
import com.gestionstages.model.TypeNotification;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.NotificationEtudiantRepository;
import com.gestionstages.repository.TacheAssigneeRepository;
import com.gestionstages.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TacheAssigneeService {
    private final TacheAssigneeRepository tacheRepository;
    private final CandidatureRepository candidatureRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final TacheAttachmentStorageService attachmentStorage;
    private final NotificationEtudiantRepository notificationRepository;

    public TacheAssigneeService(
            TacheAssigneeRepository tacheRepository,
            CandidatureRepository candidatureRepository,
            EntrepriseRepository entrepriseRepository,
            UserRepository userRepository,
            TacheAttachmentStorageService attachmentStorage,
            NotificationEtudiantRepository notificationRepository
    ) {
        this.tacheRepository = tacheRepository;
        this.candidatureRepository = candidatureRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.userRepository = userRepository;
        this.attachmentStorage = attachmentStorage;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public TacheAssigneeResponseDTO assignerTache(
            Long stageId,
            CreateTacheAssigneeDTO dto,
            Utilisateur utilisateur
    ) {
        exigerRole(utilisateur, Role.ENTREPRISE);
        Candidature stage = candidatureRepository.findByIdForUpdate(stageId)
                .orElseThrow(this::stageIntrouvable);
        exigerStageAccepte(stage);
        exigerStageActif(stage);
        exigerEntrepriseProprietaireOffre(stage, utilisateur);

        if (stage.getEncadrant() == null) {
            stage.setEncadrant(utilisateur);
            candidatureRepository.save(stage);
        }

        TacheAssignee tache = new TacheAssignee();
        tache.setStage(stage);
        tache.setEncadrant(utilisateur);
        tache.setTitre(dto.titre().trim());
        tache.setDescription(dto.description().trim());
        tache.setDateEcheance(dto.dateEcheance());
        tache.setStatut(StatutTacheAssignee.ASSIGNEE);
        tache.setDateAssignation(LocalDateTime.now());
        TacheAssignee saved = tacheRepository.saveAndFlush(tache);
        notifierAssignation(saved);
        return response(saved);
    }

    private void notifierAssignation(TacheAssignee tache) {
        Candidature stage = tache.getStage();
        NotificationEtudiant notification = new NotificationEtudiant();
        notification.setEtudiant(stage.getEtudiant());
        notification.setCandidature(stage);
        notification.setTacheAssignee(tache);
        notification.setType(TypeNotification.TACHE_ASSIGNEE);
        notification.setMessage(tronquerMessage(
                stage.getOffre().getEntreprise().getNom()
                        + " vous a assigné une nouvelle tâche : « "
                        + tache.getTitre()
                        + " »."
        ));
        notification.setLue(false);
        notification.setDateCreation(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public TacheAssigneeResponseDTO marquerCommeTerminee(
            Long stageId,
            Long tacheId,
            CompleterTacheDTO dto,
            MultipartFile pieceJointe,
            Utilisateur utilisateur
    ) {
        exigerRole(utilisateur, Role.ETUDIANT);
        TacheAssignee tache = tacheDuStage(stageId, tacheId);
        exigerStageAccepte(tache.getStage());
        exigerEtudiant(tache.getStage(), utilisateur);
        if (tache.getStatut() != StatutTacheAssignee.ASSIGNEE
                && tache.getStatut() != StatutTacheAssignee.EN_COURS) {
            throw transitionInvalide("Seule une tâche assignée ou en cours peut être terminée");
        }

        tache.setCommentaireEtudiant(nettoyer(dto.commentaireEtudiant()));
        if (pieceJointe != null && !pieceJointe.isEmpty()) {
            TacheAttachmentStorageService.StoredAttachment stored =
                    attachmentStorage.store(tache.getId(), pieceJointe);
            String anciennePieceJointe = tache.getPieceJointeEtudiant();
            tache.setPieceJointeEtudiant(stored.storageKey());
            attachmentStorage.delete(anciennePieceJointe);
        }
        tache.setStatut(StatutTacheAssignee.TERMINEE);
        tache.setDateCompletion(LocalDateTime.now());
        tache.setDateValidation(null);
        return response(tacheRepository.save(tache));
    }

    @Transactional
    public TacheAssigneeResponseDTO marquerCommeTerminee(
            Long stageId,
            Long tacheId,
            CompleterTacheDTO dto,
            Utilisateur utilisateur
    ) {
        return marquerCommeTerminee(stageId, tacheId, dto, null, utilisateur);
    }

    @Transactional
    public TacheAssigneeResponseDTO validerTache(
            Long stageId,
            Long tacheId,
            ValiderTacheDTO dto,
            Utilisateur utilisateur
    ) {
        TacheAssignee tache = tachePourEncadrant(stageId, tacheId, utilisateur);
        exigerTerminee(tache, "Impossible de valider une tâche non terminée");
        tache.setCommentaireEncadrant(nettoyer(dto.commentaireEncadrant()));
        tache.setStatut(StatutTacheAssignee.VALIDEE);
        tache.setDateValidation(LocalDateTime.now());
        return response(tacheRepository.save(tache));
    }

    @Transactional
    public TacheAssigneeResponseDTO rejeterTache(
            Long stageId,
            Long tacheId,
            ValiderTacheDTO dto,
            Utilisateur utilisateur
    ) {
        TacheAssignee tache = tachePourEncadrant(stageId, tacheId, utilisateur);
        exigerTerminee(tache, "Impossible de rejeter une tâche non terminée");
        String commentaire = nettoyer(dto.commentaireEncadrant());
        if (commentaire == null) {
            throw transitionInvalide("Le commentaire de rejet est obligatoire");
        }
        tache.setCommentaireEncadrant(commentaire);
        tache.setStatut(StatutTacheAssignee.EN_COURS);
        tache.setDateValidation(null);
        return response(tacheRepository.save(tache));
    }

    @Transactional(readOnly = true)
    public List<TacheAssigneeResponseDTO> listerTachesParStage(
            Long stageId,
            StatutTacheAssignee statutFiltre,
            Utilisateur utilisateur
    ) {
        Candidature stage = stage(stageId);
        exigerStageAccepte(stage);
        exigerAccesLecture(stage, utilisateur);
        List<TacheAssignee> taches = statutFiltre == null
                ? tacheRepository.findByStageId(stageId)
                : tacheRepository.findByStageIdAndStatut(stageId, statutFiltre);
        return taches.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public TacheAttachmentStorageService.AttachmentDocument telechargerPieceJointe(
            Long stageId,
            Long tacheId,
            Utilisateur utilisateur
    ) {
        TacheAssignee tache = tacheDuStage(stageId, tacheId);
        exigerStageAccepte(tache.getStage());
        exigerAccesLecture(tache.getStage(), utilisateur);
        if (tache.getPieceJointeEtudiant() == null) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Cette tâche n'a pas de pièce jointe");
        }
        return attachmentStorage.load(tache.getPieceJointeEtudiant());
    }

    private TacheAssignee tachePourEncadrant(
            Long stageId,
            Long tacheId,
            Utilisateur utilisateur
    ) {
        exigerRole(utilisateur, Role.ENTREPRISE);
        TacheAssignee tache = tacheDuStage(stageId, tacheId);
        exigerStageAccepte(tache.getStage());
        exigerStageActif(tache.getStage());
        exigerEntrepriseProprietaireOffre(tache.getStage(), utilisateur);
        return tache;
    }

    private TacheAssignee tacheDuStage(Long stageId, Long tacheId) {
        TacheAssignee tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new StageBusinessException(HttpStatus.NOT_FOUND, "Tâche introuvable"));
        if (!Objects.equals(tache.getStage().getId(), stageId)) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Tâche introuvable pour ce stage");
        }
        return tache;
    }

    private Candidature stage(Long stageId) {
        return candidatureRepository.findById(stageId).orElseThrow(this::stageIntrouvable);
    }

    private void exigerStageAccepte(Candidature stage) {
        if (stage.getStatut() != Candidature.StatutCandidature.ACCEPTEE) {
            throw transitionInvalide("La candidature ne représente pas un stage accepté");
        }
    }

    private void exigerEntrepriseProprietaireOffre(Candidature stage, Utilisateur utilisateur) {
        Entreprise entreprise = entrepriseUtilisateur(utilisateur);
        if (stage.getOffre().getEntreprise() == null
                || !Objects.equals(entreprise.getId(), stage.getOffre().getEntreprise().getId())) {
            throw interdit("L'entreprise ne possède pas l'offre de ce stage");
        }
    }

    private Entreprise entrepriseUtilisateur(Utilisateur utilisateur) {
        exigerRole(utilisateur, Role.ENTREPRISE);
        Entreprise entreprise = utilisateur.getEntreprise();
        if (entreprise == null) {
            entreprise = entrepriseRepository.findByEmailContact(utilisateur.getEmail()).orElse(null);
            if (entreprise != null) {
                utilisateur.setEntreprise(entreprise);
                userRepository.save(utilisateur);
            }
        }
        if (entreprise == null) {
            throw interdit("Aucune entreprise associée à ce compte");
        }
        return entreprise;
    }

    private void exigerStageActif(Candidature stage) {
        if (stage.getStatutStage() == StatutStage.CLOTURE) {
            throw new StageBusinessException(
                    HttpStatus.CONFLICT,
                    "Ce stage est clôturé, aucune nouvelle tâche ne peut être assignée"
            );
        }
    }

    private String tronquerMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }

    private void exigerEtudiant(Candidature stage, Utilisateur utilisateur) {
        if (!memeId(stage.getEtudiant(), utilisateur)) {
            throw interdit("Cette tâche appartient à un autre étudiant");
        }
    }

    private void exigerAccesLecture(Candidature stage, Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() == null) {
            throw interdit("Utilisateur non authentifié");
        }
        switch (utilisateur.getRole()) {
            case ETUDIANT -> exigerEtudiant(stage, utilisateur);
            case ENTREPRISE -> exigerEntrepriseProprietaireOffre(stage, utilisateur);
            case CHEF_DEPT_STAGE -> {
                // Le chef de département stage peut consulter tous les stages acceptés.
            }
            default -> throw interdit("Vous ne pouvez pas consulter les tâches de ce stage");
        }
    }

    private void exigerRole(Utilisateur utilisateur, Role role) {
        if (utilisateur == null || utilisateur.getRole() != role) {
            throw interdit("Rôle insuffisant pour cette opération");
        }
    }

    private void exigerTerminee(TacheAssignee tache, String message) {
        if (tache.getStatut() != StatutTacheAssignee.TERMINEE) {
            throw transitionInvalide(message);
        }
    }

    private boolean memeId(Utilisateur gauche, Utilisateur droite) {
        return gauche != null && droite != null && Objects.equals(gauche.getId(), droite.getId());
    }

    private String nettoyer(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    private TacheAssigneeResponseDTO response(TacheAssignee tache) {
        return TacheAssigneeResponseDTO.from(
                tache,
                attachmentStorage.originalFilename(tache.getPieceJointeEtudiant())
        );
    }

    private StageBusinessException interdit(String message) {
        return new StageBusinessException(HttpStatus.FORBIDDEN, message);
    }

    private StageBusinessException transitionInvalide(String message) {
        return new StageBusinessException(HttpStatus.BAD_REQUEST, message);
    }

    private StageBusinessException stageIntrouvable() {
        return new StageBusinessException(HttpStatus.NOT_FOUND, "Stage introuvable");
    }
}
