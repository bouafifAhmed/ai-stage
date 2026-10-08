package com.gestionstages.repository;

import com.gestionstages.model.OffreStage;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OffreStageRepository extends JpaRepository<OffreStage, Long> {
    Page<OffreStage> findAllByEntrepriseId(Long entrepriseId, Pageable pageable);
    Optional<OffreStage> findByIdAndEntrepriseId(Long id, Long entrepriseId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OffreStage o where o.id = :id and o.entreprise.id = :entrepriseId")
    Optional<OffreStage> findByIdAndEntrepriseIdForUpdate(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId
    );
    Page<OffreStage> findAllByStatut(OffreStage.StatutOffre statut, Pageable pageable);
    Optional<OffreStage> findByIdAndStatut(Long id, OffreStage.StatutOffre statut);
}
