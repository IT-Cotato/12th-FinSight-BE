package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiQuizSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiQuizSetRepository extends JpaRepository<AiQuizSetEntity, Long> {
    boolean existsByJobId(Long jobId);
}
