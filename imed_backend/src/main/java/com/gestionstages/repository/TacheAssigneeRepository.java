package com.gestionstages.repository;

import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.TacheAssignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TacheAssigneeRepository extends JpaRepository<TacheAssignee, Long> {
    void deleteByStageOffreId(Long offreId);

    List<TacheAssignee> findByStageId(Long stageId);

    List<TacheAssignee> findByStageIdAndStatut(Long stageId, StatutTacheAssignee statut);

    @Query("""
            select t from TacheAssignee t
            join fetch t.stage s
            join fetch s.etudiant e
            join fetch s.offre o
            join fetch o.entreprise en
            where s.id = :stageId
              and t.dateEcheance between :debut and :fin
            order by t.dateEcheance asc, t.titre asc
            """)
    List<TacheAssignee> findEcheancesByStageId(
            @Param("stageId") Long stageId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin
    );

    @Query("""
            select t from TacheAssignee t
            join fetch t.stage s
            join fetch s.etudiant e
            join fetch s.offre o
            join fetch o.entreprise en
            where s.etudiant.id = :etudiantId
              and s.statut = com.gestionstages.model.Candidature.StatutCandidature.ACCEPTEE
              and t.dateEcheance between :debut and :fin
            order by t.dateEcheance asc, t.titre asc
            """)
    List<TacheAssignee> findEcheancesByEtudiantId(
            @Param("etudiantId") Long etudiantId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin
    );
}
