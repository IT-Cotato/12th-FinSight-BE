package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import com.finsight.finsight.domain.ai.persistence.entity.AiQuizSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiQuizSetRepository extends JpaRepository<AiQuizSetEntity, Long> {
    boolean existsByJobId(Long jobId);
    Optional<AiQuizSetEntity> findTopByArticleIdAndQuizKindOrderByCreatedAtDesc(Long articleId, AiJobType quizKind);
}
