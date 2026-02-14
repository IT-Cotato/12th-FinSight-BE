package com.finsight.finsight.domain.ai.persistence.repository;


import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiJobRepositoryImpl implements AiJobRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Long> lockNextPendingIds(AiJobType type, int batchSize) {
        String sql = """
            SELECT ai_job_id
              FROM ai_job
             WHERE status = 'PENDING'
               AND job_type = :jobType
               AND (next_run_at IS NULL OR next_run_at <= SYSTIMESTAMP)
             ORDER BY requested_at
             FOR UPDATE SKIP LOCKED
            """;

        @SuppressWarnings("unchecked")
        List<Object> rows = em.createNativeQuery(sql)
                .setParameter("jobType", type.name())
                .setMaxResults(batchSize)
                .getResultList();

        List<Long> ids = new ArrayList<>();
        for (Object r : rows) {
            ids.add(((Number) r).longValue());
        }
        return ids;
    }
}

