package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiArticleInsightEntity;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiArticleInsightRepository extends JpaRepository<AiArticleInsightEntity, Long> {
    boolean existsByJobId(Long jobId);
    Optional<AiArticleInsightEntity> findTopByArticleIdOrderByCreatedAtDesc(Long articleId);

    Optional<AiArticleInsightEntity> findByArticleId(Long articleId);
}
