package com.gestionstages.service;

import com.gestionstages.dto.CreateTacheDTO;
import com.gestionstages.dto.ProgressionDTO;
import com.gestionstages.dto.TacheResponseDTO;
import com.gestionstages.dto.UpdateTacheDTO;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.StatutApprobation;
import com.gestionstages.model.Tache;
import com.gestionstages.repository.TacheRepository;
// Assume StageRepository exists as per user implicit context
// import com.gestionstages.repository.StageRepository; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TacheService {

    @Autowired
    private TacheRepository tacheRepository;
    
    // As StageRepository is not explicitly provided in the context, we will use EntityManager 
    // or a generic approach, but typical Spring Boot has it. We'll declare it and let spring resolve it if it exists.
    // If not, the user will create it. We will use a mock-like interface or just define it in the same package if needed.
    // I will assume the user has a repository or I should just create an interface for it.
    
    @Autowired
    private com.gestionstages.repository.StageRepository stageRepository;

    public TacheResponseDTO creerTache(Long stageId, CreateTacheDTO dto, Long etudiantConnecteId) {
        Candidature stage = stageRepository.findByIdWithDetails(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable"));

        if (stage.getEtudiant() == null || !stage.getEtudiant().getId().equals(etudiantConnecteId)) {
            throw new SecurityException("403: Vous n'êtes pas le propriétaire de ce stage");
        }

        if (stage.getOffre() != null) {
            LocalDate debut = stage.getOffre().getDateDebut();
            LocalDate fin = stage.getOffre().getDateFin();
            if (dto.getDate() != null && debut != null && fin != null) {
                if (dto.getDate().isBefore(debut) || dto.getDate().isAfter(fin)) {
                    throw new IllegalArgumentException("400: La date doit être comprise entre le " + debut + " et le " + fin);
                }
            }
        }

        Tache tache = new Tache();
        tache.setStage(stage);
        tache.setDate(dto.getDate());
        tache.setTitre(dto.getTitre());
        tache.setDescription(dto.getDescription());
        tache.setPieceJointe(dto.getPieceJointe());
        tache.setStatutApprobation(StatutApprobation.EN_ATTENTE);
        
        Tache saved = tacheRepository.save(tache);
        return mapToDTO(saved);
    }

    public TacheResponseDTO modifierTache(Long tacheId, UpdateTacheDTO dto, Long etudiantConnecteId) {
        Tache tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable"));
                
        Candidature stage = tache.getStage();
        
        if (stage == null || stage.getEtudiant() == null || !stage.getEtudiant().getId().equals(etudiantConnecteId)) {
            throw new SecurityException("403: Vous n'êtes pas le propriétaire de ce stage");
        }
        
        if (tache.getStatutApprobation() != StatutApprobation.REJETEE) {
            throw new IllegalArgumentException("400: Seule une tâche rejetée peut être modifiée");
        }

        if (stage.getOffre() != null) {
            LocalDate debut = stage.getOffre().getDateDebut();
            LocalDate fin = stage.getOffre().getDateFin();
            if (dto.getDate() != null && debut != null && fin != null) {
                if (dto.getDate().isBefore(debut) || dto.getDate().isAfter(fin)) {
                    throw new IllegalArgumentException("400: La date doit être comprise entre le " + debut + " et le " + fin);
                }
            }
        }
        
        tache.setDate(dto.getDate());
        tache.setTitre(dto.getTitre());
        tache.setDescription(dto.getDescription());
        if (dto.getPieceJointe() != null) {
            tache.setPieceJointe(dto.getPieceJointe());
        }
        tache.setStatutApprobation(StatutApprobation.EN_ATTENTE);
        
        Tache saved = tacheRepository.save(tache);
        return mapToDTO(saved);
    }

    public List<TacheResponseDTO> listerTachesParStage(Long stageId, Long utilisateurConnecteId, String role) {
        Candidature stage = stageRepository.findByIdWithDetails(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable"));
                
        // Verification d'accès
        boolean hasAccess = false;
        if ("ETUDIANT".equals(role)) {
            if (stage.getEtudiant() != null && stage.getEtudiant().getId().equals(utilisateurConnecteId)) {
                hasAccess = true;
            }
        } else if ("ENTREPRISE".equals(role)) { // Encadrant
            if (stage.getEncadrant() != null && stage.getEncadrant().getId().equals(utilisateurConnecteId)) {
                hasAccess = true;
            }
        } else if ("CHEF_DEPT_STAGE".equals(role)) {
            hasAccess = true;
        }
        
        if (!hasAccess) {
            throw new SecurityException("403: Accès non autorisé à ce journal");
        }
        
        return tacheRepository.findByStageIdOrderByDateAsc(stageId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ProgressionDTO calculerProgression(Long stageId) {
        Candidature stage = stageRepository.findByIdWithDetails(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage introuvable"));
                
        LocalDate debut = stage.getOffre() != null ? stage.getOffre().getDateDebut() : null;
        LocalDate fin = stage.getOffre() != null ? stage.getOffre().getDateFin() : null;
        long totalJours = countWorkingDays(debut, fin);
        
        // Compter les tâches APPROUVÉES et EN_ATTENTE (exclure uniquement les REJETÉES)
        List<Tache> tachesValides = tacheRepository.findByStageIdOrderByDateAsc(stageId).stream()
                .filter(t -> t.getStatutApprobation() != StatutApprobation.REJETEE)
                .collect(Collectors.toList());
        
        long joursCouverts = tachesValides.stream()
                .map(Tache::getDate)
                .distinct()
                .count();
                
        double pourcentage = totalJours == 0 ? 0 : ((double) joursCouverts / totalJours) * 100;
        
        // Log pour déboguer
        System.out.println("=== Calcul de progression pour stage " + stageId + " ===");
        System.out.println("Date début: " + debut);
        System.out.println("Date fin: " + fin);
        System.out.println("Total jours ouvrables: " + totalJours);
        System.out.println("Nombre de tâches valides: " + tachesValides.size());
        System.out.println("Jours couverts: " + joursCouverts);
        System.out.println("Pourcentage: " + pourcentage + "%");
        
        return new ProgressionDTO(totalJours, joursCouverts, pourcentage);
    }
    
    private long countWorkingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            return 0;
        }
        long days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }
    
    private TacheResponseDTO mapToDTO(Tache tache) {
        TacheResponseDTO dto = new TacheResponseDTO();
        dto.setId(tache.getId());
        dto.setStageId(tache.getStage().getId());
        dto.setDate(tache.getDate());
        dto.setTitre(tache.getTitre());
        dto.setDescription(tache.getDescription());
        dto.setPieceJointe(tache.getPieceJointe());
        dto.setStatutApprobation(tache.getStatutApprobation());
        dto.setCommentaireEncadrant(tache.getCommentaireEncadrant());
        dto.setDateSaisie(tache.getDateSaisie());
        dto.setDateApprobation(tache.getDateApprobation());
        return dto;
    }
}
