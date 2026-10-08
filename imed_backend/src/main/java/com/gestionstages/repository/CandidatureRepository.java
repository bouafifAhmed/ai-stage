package com.gestionstages.repository;

import com.gestionstages.model.Candidature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    boolean existsByOffreIdAndEtudiantId(Long offreId, Long etudiantId);
    boolean existsByOffreId(Long offreId);
    long countByOffreIdAndStatut(Long offreId, Candidature.StatutCandidature statut);
    Page<Candidature> findAllByEtudiantId(Long etudiantId, Pageable pageable);
    Page<Candidature> findAllByOffreIdAndOffreEntrepriseId(Long offreId, Long entrepriseId, Pageable pageable);
    Optional<Candidature> findByIdAndOffreEntrepriseId(Long id, Long entrepriseId);
    void deleteByOffreId(Long offreId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Candidature c where c.id = :id")
    Optional<Candidature> findByIdForUpdate(Long id);
}
