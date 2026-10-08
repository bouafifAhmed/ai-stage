package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.*;
import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.*;
import com.gestionstages.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EntrepriseStageService {
    private final OffreStageRepository offreRepository;
    private final CandidatureRepository candidatureRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final NotificationEtudiantRepository notificationRepository;
    private final TacheAssigneeRepository tacheAssigneeRepository;

    public EntrepriseStageService(OffreStageRepository offreRepository,
                                  CandidatureRepository candidatureRepository,
                                  EntrepriseRepository entrepriseRepository,
                                  UserRepository userRepository,
                                  NotificationEtudiantRepository notificationRepository,
                                  TacheAssigneeRepository tacheAssigneeRepository) {
        this.offreRepository = offreRepository;
        this.candidatureRepository = candidatureRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.tacheAssigneeRepository = tacheAssigneeRepository;
    }

    @Transactional
    public OffreResponse creer(Utilisateur user, OffreRequest request) {
        Entreprise entreprise = entrepriseValidee(user);
        validateDates(request);
        OffreStage offre = new OffreStage();
        offre.setEntreprise(entreprise);
        apply(offre, request);
        return offreResponse(offreRepository.save(offre));
    }

    @Transactional(readOnly = true)
    public Page<OffreResponse> lister(Utilisateur user, Pageable pageable) {
        Entreprise e = entrepriseValidee(user);
        return offreRepository.findAllByEntrepriseId(e.getId(), pageable).map(this::offreResponse);
    }

    @Transactional(readOnly = true)
    public OffreResponse detail(Utilisateur user, Long id) {
        return offreResponse(offrePossedee(user, id));
    }

    @Transactional
    public OffreResponse modifier(Utilisateur user, Long id, OffreRequest request) {
        validateDates(request);
        Entreprise entreprise = entrepriseValidee(user);
        OffreStage offre = offreRepository.findByIdAndEntrepriseIdForUpdate(id, entreprise.getId())
                .orElseThrow(this::notFound);
        long candidaturesAcceptees = candidatureRepository.countByOffreIdAndStatut(
                id,
                Candidature.StatutCandidature.ACCEPTEE
        );
        if (request.nombrePlaces() < candidaturesAcceptees) {
            throw new StageBusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Le nombre de places ne peut pas être inférieur au nombre de candidatures déjà acceptées"
            );
        }
        apply(offre, request);
        return offreResponse(offreRepository.save(offre));
    }

    @Transactional
    public void supprimer(Utilisateur user, Long id) {
        OffreStage offre = offrePossedee(user, id);
        notificationRepository.deleteByCandidatureOffreId(id);
        tacheAssigneeRepository.deleteByStageOffreId(id);
        candidatureRepository.deleteByOffreId(id);
        offreRepository.delete(offre);
    }

    @Transactional(readOnly = true)
    public Page<CandidatureResponse> candidatures(Utilisateur user, Long offreId, Pageable pageable) {
        Entreprise e = entrepriseValidee(user);
        offreRepository.findByIdAndEntrepriseId(offreId, e.getId()).orElseThrow(this::notFound);
        return candidatureRepository
                .findAllByOffreIdAndOffreEntrepriseId(offreId, e.getId(), pageable)
                .map(CandidatureResponse::from);
    }

    @Transactional(readOnly = true)
    public CandidatureResponse candidature(Utilisateur user, Long candidatureId) {
        Entreprise e = entrepriseValidee(user);
        return candidatureRepository.findByIdAndOffreEntrepriseId(candidatureId, e.getId())
                .map(CandidatureResponse::from).orElseThrow(this::notFound);
    }

    @Transactional
    public CandidatureResponse modifierStatutCandidature(
            Utilisateur user,
            Long candidatureId,
            CandidatureStatutRequest request
    ) {
        Entreprise e = entrepriseValidee(user);
        Candidature candidature = candidatureRepository
                .findByIdAndOffreEntrepriseId(candidatureId, e.getId())
                .orElseThrow(this::notFound);
        if (request.statut() == Candidature.StatutCandidature.EN_ATTENTE) {
            throw new StageBusinessException(
                    HttpStatus.BAD_REQUEST,
                    "La candidature doit être acceptée ou refusée"
            );
        }
        Candidature.StatutCandidature ancienStatut = candidature.getStatut();
        if (request.statut() == Candidature.StatutCandidature.ACCEPTEE
                && ancienStatut != Candidature.StatutCandidature.ACCEPTEE) {
            OffreStage offre = offreRepository.findByIdAndEntrepriseIdForUpdate(
                    candidature.getOffre().getId(),
                    e.getId()
            ).orElseThrow(this::notFound);
            long candidaturesAcceptees = candidatureRepository.countByOffreIdAndStatut(
                    offre.getId(),
                    Candidature.StatutCandidature.ACCEPTEE
            );
            if (candidaturesAcceptees >= offre.getNombrePlaces()) {
                throw new StageBusinessException(
                        HttpStatus.CONFLICT,
                        "Toutes les places de cette offre sont déjà attribuées"
                );
            }
        }
        candidature.setStatut(request.statut());
        Candidature saved = candidatureRepository.save(candidature);
        if (request.statut() == Candidature.StatutCandidature.ACCEPTEE
                && ancienStatut != Candidature.StatutCandidature.ACCEPTEE) {
            notifierAcceptation(saved);
        } else if (request.statut() == Candidature.StatutCandidature.REFUSEE) {
            notificationRepository.deleteByCandidatureId(saved.getId());
        }
        return CandidatureResponse.from(saved);
    }

    private void notifierAcceptation(Candidature candidature) {
        NotificationEtudiant notification = notificationRepository
                .findByCandidatureIdAndType(
                        candidature.getId(),
                        TypeNotification.CANDIDATURE_ACCEPTEE
                )
                .orElseGet(NotificationEtudiant::new);
        notification.setEtudiant(candidature.getEtudiant());
        notification.setCandidature(candidature);
        notification.setType(TypeNotification.CANDIDATURE_ACCEPTEE);
        notification.setTacheAssignee(null);
        notification.setMessage(
                "Félicitations ! Votre candidature pour « "
                        + candidature.getOffre().getTitre()
                        + " » chez "
                        + candidature.getOffre().getEntreprise().getNom()
                        + " a été acceptée."
        );
        notification.setLue(false);
        notification.setDateCreation(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private OffreStage offrePossedee(Utilisateur user, Long id) {
        Entreprise e = entrepriseValidee(user);
        return offreRepository.findByIdAndEntrepriseId(id, e.getId()).orElseThrow(this::notFound);
    }

    private Entreprise entrepriseValidee(Utilisateur user) {
        if (user == null || user.getRole() != Role.ENTREPRISE) {
            throw new StageBusinessException(HttpStatus.FORBIDDEN, "Accès réservé aux entreprises");
        }
        Entreprise e = user.getEntreprise();
        if (e == null) {
            e = entrepriseRepository.findByEmailContact(user.getEmail()).orElse(null);
            if (e != null) {
                user.setEntreprise(e);
                userRepository.save(user);
            }
        }
        if (e == null) {
            throw new StageBusinessException(HttpStatus.FORBIDDEN, "Aucune entreprise associée à ce compte");
        }
        if (e.getStatutValidation() != StatutValidation.VALIDEE || !user.isActif()) {
            throw new StageBusinessException(HttpStatus.FORBIDDEN, "L'entreprise doit être validée");
        }
        return e;
    }

    private void validateDates(OffreRequest r) {
        if (r.dateDebut() != null && r.dateFin() != null && r.dateFin().isBefore(r.dateDebut())) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "La date de fin précède la date de début");
        }
        if (r.statut() == OffreStage.StatutOffre.PUBLIEE && r.dateLimite().isBefore(LocalDate.now())) {
            throw new StageBusinessException(HttpStatus.BAD_REQUEST, "La date limite d'une offre publiée est dépassée");
        }
    }

    private void apply(OffreStage o, OffreRequest r) {
        o.setTitre(r.titre().trim()); o.setDescription(r.description().trim());
        o.setDomaine(r.domaine().trim()); o.setLocalisation(r.localisation().trim());
        o.setMode(r.mode()); o.setTypeStage(r.typeStage().trim()); o.setDureeMois(r.dureeMois());
        o.setDateDebut(r.dateDebut()); o.setDateFin(r.dateFin()); o.setDateLimite(r.dateLimite());
        o.setCompetences(r.competences()); o.setNiveauRequis(clean(r.niveauRequis()));
        o.setNombrePlaces(r.nombrePlaces()); o.setRemuneration(r.remuneration()); o.setStatut(r.statut());
    }

    private OffreResponse offreResponse(OffreStage offre) {
        long placesOccupees = candidatureRepository.countByOffreIdAndStatut(
                offre.getId(),
                Candidature.StatutCandidature.ACCEPTEE
        );
        return OffreResponse.from(offre, placesOccupees);
    }

    private String clean(String value) { return value == null ? null : value.trim(); }
    private StageBusinessException notFound() {
        return new StageBusinessException(HttpStatus.NOT_FOUND, "Ressource introuvable");
    }
}
