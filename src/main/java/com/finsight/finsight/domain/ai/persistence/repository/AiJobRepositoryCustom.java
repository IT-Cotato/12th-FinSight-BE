package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;

import java.util.List;

public interface AiJobRepositoryCustom {
    List<Long> lockNextPendingIds(AiJobType type, int batchSize);
}
