package com.gestionstages.repository;

import com.gestionstages.model.OptionReponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionReponseRepository extends JpaRepository<OptionReponse, Long> {
    java.util.List<OptionReponse> findByQuestionIdOrderByOrdreAsc(Long questionId);

    boolean existsByIdAndQuestionId(Long id, Long questionId);
}
