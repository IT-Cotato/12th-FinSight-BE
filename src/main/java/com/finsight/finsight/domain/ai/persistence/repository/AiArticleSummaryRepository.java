package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiArticleSummaryEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiArticleSummaryRepository extends JpaRepository<AiArticleSummaryEntity, Long> {
    Optional<AiArticleSummaryEntity> findTopByArticleIdOrderByCreatedAtDesc(Long articleId);
    boolean existsByJobId(Long jobId);
    Optional<AiArticleSummaryEntity> findByJobId(Long jobId);
    Optional<AiArticleSummaryEntity> findTopByArticleIdAndJobStatusOrderByCreatedAtDesc(Long articleId, AiJobStatus status);
}