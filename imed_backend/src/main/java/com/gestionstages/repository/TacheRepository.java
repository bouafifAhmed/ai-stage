package com.gestionstages.repository;

import com.gestionstages.model.StatutApprobation;
import com.gestionstages.model.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {
    
    List<Tache> findByStageIdOrderByDateAsc(Long stageId);
    
    List<Tache> findByStageIdAndStatutApprobation(Long stageId, StatutApprobation statut);
    
    long countByStageIdAndStatutApprobation(Long stageId, StatutApprobation statut);
}
