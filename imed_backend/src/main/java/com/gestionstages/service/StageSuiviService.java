package com.gestionstages.service;

import com.gestionstages.dto.StageSuiviDTOs;
import com.gestionstages.dto.StageSuiviDTOs.EcheanceCalendrierDTO;
import com.gestionstages.dto.StageSuiviDTOs.SignerStageRequest;
import com.gestionstages.dto.StageSuiviDTOs.StageClotureResponseDTO;
import com.gestionstages.dto.StageSuiviDTOs.UtilisateurContext;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutStage;
import com.gestionstages.model.TacheAssignee;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.CandidatureRepository;
import com.gestionstages.repository.TacheAssigneeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
public class StageSuiviService {
    private final CandidatureRepository candidatureRepository;
    private final TacheAssigneeRepository tacheRepository;
    private final SignatureStorageService signatureStorage;
    private final StageRapportPdfService rapportPdfService;

    public StageSuiviService(
            CandidatureRepository candidatureRepository,
            TacheAssigneeRepository tacheRepository,
            SignatureStorageService signatureStorage,
            StageRapportPdfService rapportPdfService
    ) {
        this.candidatureRepository = candidatureRepository;
        this.tacheRepository = tacheRepository;
        this.signatureStorage = signatureStorage;
        this.rapportPdfService = rapportPdfService;
    }

    @Transactional(readOnly = true)
    public List<EcheanceCalendrierDTO> echeancesStage(
            Long stageId,
            int annee,
            int mois,
            Utilisateur utilisateur
    ) {
        Candidature stage = stage(stageId);
        exigerAccesLecture(stage, utilisateur);
        return echeancesPeriode(
                tacheRepository.findEcheancesByStageId(stageId, debutMois(annee, mois), finMois(annee, mois))
        );
    }

    @Transactional(readOnly = true)
    public List<EcheanceCalendrierDTO> echeancesEtudiant(
            int annee,
            int mois,
            Utilisateur utilisateur
    ) {
        exigerRole(utilisateur, Role.ETUDIANT);
        return echeancesPeriode(
                tacheRepository.findEcheancesByEtudiantId(
                        utilisateur.getId(),
                        debutMois(annee, mois),
                        finMois(annee, mois)
                )
        );
    }

    @Transactional(readOnly = true)
    public byte[] telechargerRapport(Long stageId, Utilisateur utilisateur) {
        Candidature stage = stage(stageId);
        exigerAccesLecture(stage, utilisateur);
        List<TacheAssignee> taches = tacheRepository.findByStageId(stageId);
        return rapportPdfService.generer(stage, taches);
    }

    @Transactional(readOnly = true)
    public StageClotureResponseDTO etatCloture(Long stageId, Utilisateur utilisateur) {
        Candidature stage = stage(stageId);
        exigerAccesLecture(stage, utilisateur);
        return StageClotureResponseDTO.from(stage, contexteSignature(stage, utilisateur));
    }

    @Transactional
    public StageClotureResponseDTO signerEtudiant(
            Long stageId,
            SignerStageRequest request,
            Utilisateur utilisateur
    ) {
        exigerRole(utilisateur, Role.ETUDIANT);
        Candidature stage = candidatureRepository.findByIdForUpdate(stageId)
                .orElseThrow(this::stageIntrouvable);
        exigerStageAccepte(stage);
        exigerEtudiant(stage, utilisateur);
        exigerStageActif(stage);

        if (stage.getSignatureEtudiantPath() != null) {
            throw transitionInvalide("La signature étudiant est déjà enregistrée");
        }

        String storageKey = signatureStorage.store(stageId, "etudiant", request.signatureBase64());
        stage.setSignatureEtudiantPath(storageKey);
        stage.setDateSignatureEtudiant(LocalDateTime.now());
        cloturerSiComplet(stage);
        candidatureRepository.save(stage);
        return StageClotureResponseDTO.from(stage, contexteSignature(stage, utilisateur));
    }

    @Transactional
    public StageClotureResponseDTO signerEncadrant(
            Long stageId,
            SignerStageRequest request,
            Utilisateur utilisateur
    ) {
        exigerRole(utilisateur, Role.ENTREPRISE);
        Candidature stage = candidatureRepository.findByIdForUpdate(stageId)
                .orElseThrow(this::stageIntrouvable);
        exigerStageAccepte(stage);
        exigerEntrepriseProprietaireOffre(stage, utilisateur);
        exigerStageActif(stage);

        if (stage.getEncadrant() == null) {
            stage.setEncadrant(utilisateur);
        } else {
            exigerEncadrant(stage, utilisateur);
        }

        if (stage.getSignatureEncadrantPath() != null) {
            throw transitionInvalide("La signature encadrant est déjà enregistrée");
        }

        String storageKey = signatureStorage.store(stageId, "encadrant", request.signatureBase64());
        stage.setSignatureEncadrantPath(storageKey);
        stage.setDateSignatureEncadrant(LocalDateTime.now());
        cloturerSiComplet(stage);
        candidatureRepository.save(stage);
        return StageClotureResponseDTO.from(stage, contexteSignature(stage, utilisateur));
    }

    private void cloturerSiComplet(Candidature stage) {
        if (stage.getSignatureEtudiantPath() != null && stage.getSignatureEncadrantPath() != null) {
            stage.setStatutStage(StatutStage.CLOTURE);
            if (stage.getDateCloture() == null) {
                stage.setDateCloture(LocalDateTime.now());
            }
        }
    }

    private List<EcheanceCalendrierDTO> echeancesPeriode(List<TacheAssignee> taches) {
        return taches.stream().map(this::toEcheance).toList();
    }

    private EcheanceCalendrierDTO toEcheance(TacheAssignee tache) {
        Candidature stage = tache.getStage();
        return new EcheanceCalendrierDTO(
                tache.getId(),
                stage.getId(),
                tache.getTitre(),
                tache.getDateEcheance(),
                tache.getStatut(),
                nomComplet(stage.getEtudiant().getPrenom(), stage.getEtudiant().getNom()),
                stage.getOffre().getEntreprise().getNom(),
                stage.getOffre().getTitre()
        );
    }

    private UtilisateurContext contexteSignature(Candidature stage, Utilisateur utilisateur) {
        boolean peutSignerEtudiant = utilisateur != null
                && utilisateur.getRole() == Role.ETUDIANT
                && memeId(stage.getEtudiant(), utilisateur)
                && stage.getStatutStage() == StatutStage.ACTIF
                && stage.getSignatureEtudiantPath() == null;

        boolean peutSignerEncadrant = utilisateur != null
                && utilisateur.getRole() == Role.ENTREPRISE
                && stage.getStatutStage() == StatutStage.ACTIF
                && stage.getSignatureEncadrantPath() == null
                && utilisateur.getEntreprise() != null
                && stage.getOffre().getEntreprise() != null
                && Objects.equals(
                        utilisateur.getEntreprise().getId(),
                        stage.getOffre().getEntreprise().getId()
                )
                && (stage.getEncadrant() == null || memeId(stage.getEncadrant(), utilisateur));

        return new UtilisateurContext(peutSignerEtudiant, peutSignerEncadrant);
    }

    private LocalDate debutMois(int annee, int mois) {
        return YearMonth.of(annee, mois).atDay(1);
    }

    private LocalDate finMois(int annee, int mois) {
        return YearMonth.of(annee, mois).atEndOfMonth();
    }

    private Candidature stage(Long stageId) {
        return candidatureRepository.findById(stageId).orElseThrow(this::stageIntrouvable);
    }

    private void exigerStageAccepte(Candidature stage) {
        if (stage.getStatut() != Candidature.StatutCandidature.ACCEPTEE) {
            throw transitionInvalide("La candidature ne représente pas un stage accepté");
        }
    }

    private void exigerStageActif(Candidature stage) {
        if (stage.getStatutStage() == StatutStage.CLOTURE) {
            throw transitionInvalide("Ce stage est déjà clôturé");
        }
    }

    private void exigerEntrepriseProprietaireOffre(Candidature stage, Utilisateur utilisateur) {
        if (utilisateur.getEntreprise() == null
                || stage.getOffre().getEntreprise() == null
                || !Objects.equals(
                        utilisateur.getEntreprise().getId(),
                        stage.getOffre().getEntreprise().getId()
                )) {
            throw interdit("L'entreprise ne possède pas l'offre de ce stage");
        }
    }

    private void exigerEncadrant(Candidature stage, Utilisateur utilisateur) {
        if (stage.getEncadrant() == null || !memeId(stage.getEncadrant(), utilisateur)) {
            throw interdit("Vous n'êtes pas l'encadrant de ce stage");
        }
    }

    private void exigerEtudiant(Candidature stage, Utilisateur utilisateur) {
        if (!memeId(stage.getEtudiant(), utilisateur)) {
            throw interdit("Ce stage appartient à un autre étudiant");
        }
    }

    private void exigerAccesLecture(Candidature stage, Utilisateur utilisateur) {
        exigerStageAccepte(stage);
        if (utilisateur == null || utilisateur.getRole() == null) {
            throw interdit("Utilisateur non authentifié");
        }
        switch (utilisateur.getRole()) {
            case ETUDIANT -> exigerEtudiant(stage, utilisateur);
            case ENTREPRISE -> exigerEntrepriseProprietaireOffre(stage, utilisateur);
            case CHEF_DEPT_STAGE -> {
                // Lecture autorisée pour le chef de département stage.
            }
            default -> throw interdit("Vous ne pouvez pas consulter ce stage");
        }
    }

    private void exigerRole(Utilisateur utilisateur, Role role) {
        if (utilisateur == null || utilisateur.getRole() != role) {
            throw interdit("Rôle insuffisant pour cette opération");
        }
    }

    private boolean memeId(Utilisateur gauche, Utilisateur droite) {
        return gauche != null && droite != null && Objects.equals(gauche.getId(), droite.getId());
    }

    private String nomComplet(String prenom, String nom) {
        return (prenom + " " + nom).trim();
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
