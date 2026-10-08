package com.gestionstages.repository;

import com.gestionstages.model.Reclamation;
import com.gestionstages.model.StatutReclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    List<Reclamation> findByStageId(Long stageId);

    @Query("SELECT r FROM Reclamation r " +
           "JOIN FETCH r.stage s " +
           "JOIN FETCH s.offre o " +
           "JOIN FETCH o.entreprise e " +
           "LEFT JOIN FETCH r.etudiant et " +
           "LEFT JOIN FETCH r.traitePar t " +
           "WHERE e.id = :entrepriseId")
    List<Reclamation> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT r FROM Reclamation r WHERE r.statut = :statut AND r.dateCloture < :date")
    List<Reclamation> findByStatutAndDateClotureBeforeForAutoClose(
            @Param("statut") StatutReclamation statut,
            @Param("date") LocalDateTime date
    );

    @Query("SELECT r FROM Reclamation r " +
           "LEFT JOIN FETCH r.stage s " +
           "LEFT JOIN FETCH r.etudiant e " +
           "LEFT JOIN FETCH r.traitePar t " +
           "WHERE r.id = :id")
    Optional<Reclamation> findByIdWithDetails(@Param("id") Long id);
}
