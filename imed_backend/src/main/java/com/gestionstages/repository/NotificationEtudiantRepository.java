package com.gestionstages.repository;

import com.gestionstages.model.NotificationEtudiant;
import com.gestionstages.model.TypeNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationEtudiantRepository extends JpaRepository<NotificationEtudiant, Long> {
    Page<NotificationEtudiant> findAllByEtudiantId(Long etudiantId, Pageable pageable);
    Optional<NotificationEtudiant> findByIdAndEtudiantId(Long id, Long etudiantId);
    Optional<NotificationEtudiant> findByCandidatureIdAndType(
            Long candidatureId,
            TypeNotification type
    );
    long countByEtudiantIdAndLueFalse(Long etudiantId);
    void deleteByCandidatureId(Long candidatureId);
    void deleteByCandidatureOffreId(Long offreId);
}
