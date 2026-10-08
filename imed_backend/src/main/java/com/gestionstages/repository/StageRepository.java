package com.gestionstages.repository;

import com.gestionstages.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StageRepository extends JpaRepository<Candidature, Long> {
    
    @Query("SELECT c FROM Candidature c " +
           "LEFT JOIN FETCH c.offre o " +
           "LEFT JOIN FETCH o.entreprise " +
           "LEFT JOIN FETCH c.etudiant " +
           "LEFT JOIN FETCH c.encadrant " +
           "WHERE c.id = :id")
    Optional<Candidature> findByIdWithDetails(@Param("id") Long id);
}
