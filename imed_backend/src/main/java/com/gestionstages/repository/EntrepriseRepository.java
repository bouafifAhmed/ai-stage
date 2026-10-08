package com.gestionstages.repository;

import com.gestionstages.model.Entreprise;
import com.gestionstages.model.StatutValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
    boolean existsByEmailContact(String email);

    Optional<Entreprise> findByEmailContact(String email);

    boolean existsByNom(String nom);

    Page<Entreprise> findAllByStatutValidation(
            StatutValidation statutValidation,
            Pageable pageable
    );
}
