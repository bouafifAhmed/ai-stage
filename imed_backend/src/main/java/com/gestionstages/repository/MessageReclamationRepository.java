package com.gestionstages.repository;

import com.gestionstages.model.MessageReclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageReclamationRepository extends JpaRepository<MessageReclamation, Long> {

    @Query("SELECT m FROM MessageReclamation m " +
           "LEFT JOIN FETCH m.auteur " +
           "WHERE m.reclamation.id = :reclamationId " +
           "ORDER BY m.dateEnvoi ASC")
    List<MessageReclamation> findByReclamationIdOrderByDateEnvoiAsc(@Param("reclamationId") Long reclamationId);
}
