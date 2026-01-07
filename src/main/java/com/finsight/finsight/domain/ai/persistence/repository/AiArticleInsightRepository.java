package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiArticleInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiArticleInsightRepository extends JpaRepository<AiArticleInsightEntity, Long> {
    boolean existsByJobId(Long jobId);
}
