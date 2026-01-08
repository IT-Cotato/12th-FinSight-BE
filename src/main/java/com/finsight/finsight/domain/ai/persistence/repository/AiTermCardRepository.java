package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiTermCardRepository extends JpaRepository<AiTermCardEntity, Long> {
    boolean existsByJobId(Long jobId);
    // 성능/N+1 방지: EntityGraph -> 카드 조회할 때 term 같이 가져옴
    List<AiTermCardEntity> findByArticleIdOrderByCardOrderAsc(Long articleId);
}
