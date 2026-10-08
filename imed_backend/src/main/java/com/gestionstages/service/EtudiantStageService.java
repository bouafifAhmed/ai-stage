package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.*;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EtudiantStageService {
    private final OffreStageRepository offreRepository;
    private final CandidatureRepository candidatureRepository;
    private final UserRepository userRepository;
    private final CvStorageService cvStorageService;
    private final CvExtractionService cvExtractionService;
    private final NotificationEtudiantRepository notificationRepository;
    private final QcmEtudiantService qcmEtudiantService;

    public EtudiantStageService(OffreStageRepository offreRepository,
                                CandidatureRepository candidatureRepository,
                                UserRepository userRepository,
                                CvStorageService cvStorageService,
                                CvExtractionService cvExtractionService,
                                NotificationEtudiantRepository notificationRepository,
                                QcmEtudiantService qcmEtudiantService) {
        this.offreRepository = offreRepository;
        this.candidatureRepository = candidatureRepository;
        this.userRepository = userRepository;
        this.cvStorageService = cvStorageService;
        this.cvExtractionService = cvExtractionService;
        this.notificationRepository = notificationRepository;
        this.qcmEtudiantService = qcmEtudiantService;
    }

    @Transactional(readOnly = true)
    public Page<OffreResponse> offres(Utilisateur user, Pageable pageable) {
        etudiant(user);
        return offreRepository.findAllByStatut(OffreStage.StatutOffre.PUBLIEE, pageable)
                .map(this::offreResponse);
    }

    @Transactional(readOnly = true)
    public OffreResponse offre(Utilisateur user, Long id) {
        etudiant(user);
        return offreResponse(offreRepository.findByIdAndStatut(id, OffreStage.StatutOffre.PUBLIEE)
                .orElseThrow(this::notFound));
    }

    @Transactional(readOnly = true)
    public ProfilResponse profil(Utilisateur user) {
        return ProfilResponse.from(etudiant(user));
    }

    @Transactional
    public ProfilResponse modifierProfil(Utilisateur user, ProfilRequest request) {
        Utilisateur e = etudiant(user);
        e.setTelephone(clean(request.telephone()));
        e.setFiliere(clean(request.filiere()));
        e.setNiveauEtudes(clean(request.niveauEtudes()));
        e.setCompetences(request.competences());
        return ProfilResponse.from(userRepository.save(e));
    }

    @Transactional
    public CvUploadResponse enregistrerCv(Utilisateur user, MultipartFile file) {
        Utilisateur e = etudiant(user);
        cvStorageService.store(e.getId(), file);
        e.setCvNomFichier(cvStorageService.safeOriginalFilename(file));
        e.setCvDateDepot(LocalDateTime.now());
        ProfilResponse profil = ProfilResponse.from(userRepository.save(e));
        return new CvUploadResponse(profil, cvExtractionService.extract(readFile(file)));
    }

    @Transactional(readOnly = true)
    public CvExtractionResponse extraireCv(Utilisateur user) {
        Utilisateur e = etudiant(user);
        if (e.getCvNomFichier() == null) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Aucun CV n'a été déposé");
        }
        try (InputStream input = cvStorageService.load(e.getId()).getInputStream()) {
            return cvExtractionService.extract(input);
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le CV n'a pas pu être analysé"
            );
        }
    }

    @Transactional(readOnly = true)
    public CvDocument telechargerCv(Utilisateur user) {
        Utilisateur e = etudiant(user);
        if (e.getCvNomFichier() == null) {
            throw new StageBusinessException(HttpStatus.NOT_FOUND, "Aucun CV n'a été déposé");
        }
        return new CvDocument(cvStorageService.load(e.getId()), e.getCvNomFichier());
    }

    @Transactional
    public void supprimerCv(Utilisateur user) {
        Utilisateur e = etudiant(user);
        cvStorageService.delete(e.getId());
        e.setCvNomFichier(null);
        e.setCvDateDepot(null);
        userRepository.save(e);
    }

    @Transactional
    public CandidatureResponse postuler(Utilisateur user, CandidatureRequest request) {
        Utilisateur e = etudiant(user);
        qcmEtudiantService.verifierAdmissibilite(e);
        Long offreId = request.offreId();
        OffreStage offre = offreRepository.findByIdAndStatut(offreId, OffreStage.StatutOffre.PUBLIEE)
                .orElseThrow(this::notFound);
        if (offre.getDateLimite().isBefore(LocalDate.now())) {
            throw new StageBusinessException(HttpStatus.CONFLICT, "La date limite de candidature est dépassée");
        }
        if (candidatureRepository.existsByOffreIdAndEtudiantId(offreId, e.getId())) {
            throw new StageBusinessException(HttpStatus.CONFLICT, "Vous avez déjà postulé à cette offre");
        }
        Candidature candidature = new Candidature();
        candidature.setOffre(offre);
        candidature.setEtudiant(e);
        candidature.setMessage(clean(request.message()));
        candidature.setStatut(Candidature.StatutCandidature.EN_ATTENTE);
        return CandidatureResponse.from(candidatureRepository.save(candidature));
    }

    @Transactional(readOnly = true)
    public Page<CandidatureResponse> candidatures(Utilisateur user, Pageable pageable) {
        Utilisateur e = etudiant(user);
        return candidatureRepository.findAllByEtudiantId(e.getId(), pageable)
                .map(CandidatureResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> notifications(Utilisateur user, Pageable pageable) {
        Utilisateur e = etudiant(user);
        return notificationRepository.findAllByEtudiantId(e.getId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse notificationsNonLues(Utilisateur user) {
        Utilisateur e = etudiant(user);
        return new NotificationCountResponse(
                notificationRepository.countByEtudiantIdAndLueFalse(e.getId())
        );
    }

    @Transactional
    public NotificationResponse marquerNotificationLue(Utilisateur user, Long notificationId) {
        Utilisateur e = etudiant(user);
        NotificationEtudiant notification = notificationRepository
                .findByIdAndEtudiantId(notificationId, e.getId())
                .orElseThrow(() -> new StageBusinessException(
                        HttpStatus.NOT_FOUND,
                        "Notification introuvable"
                ));
        notification.setLue(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    private Utilisateur etudiant(Utilisateur user) {
        if (user == null || user.getRole() != Role.ETUDIANT) {
            throw new StageBusinessException(HttpStatus.FORBIDDEN, "Accès réservé aux étudiants");
        }
        return user;
    }
    private OffreResponse offreResponse(OffreStage offre) {
        long placesOccupees = candidatureRepository.countByOffreIdAndStatut(
                offre.getId(),
                Candidature.StatutCandidature.ACCEPTEE
        );
        return OffreResponse.from(offre, placesOccupees);
    }
    private String clean(String value) { return value == null ? null : value.trim(); }
    private byte[] readFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            return new byte[0];
        }
    }
    private StageBusinessException notFound() {
        return new StageBusinessException(HttpStatus.NOT_FOUND, "Offre publiée introuvable");
    }

    public record CvDocument(Resource resource, String filename) {}
}
