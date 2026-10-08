package com.gestionstages.repository;

import com.gestionstages.model.Qcm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QcmRepository extends JpaRepository<Qcm, Long> {
    java.util.Optional<Qcm> findFirstByActifTrueOrderByIdAsc();
}
