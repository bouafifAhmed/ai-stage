package com.gestionstages.repository;

import com.gestionstages.model.TentativeQcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TentativeQcmRepository extends JpaRepository<TentativeQcm, Long> {
    long countByEtudiantIdAndQcmId(Long etudiantId, Long qcmId);

    List<TentativeQcm> findByEtudiantIdAndQcmIdOrderByDateSoumissionDesc(
            Long etudiantId,
            Long qcmId
    );

    boolean existsByEtudiantIdAndQcmIdAndReussiTrue(Long etudiantId, Long qcmId);

    Optional<TentativeQcm> findFirstByEtudiantIdAndQcmIdAndReussiTrue(
            Long etudiantId,
            Long qcmId
    );
}
