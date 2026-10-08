package com.gestionstages.repository;

import com.gestionstages.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    java.util.List<Question> findByQcmIdOrderByOrdreAsc(Long qcmId);
}
