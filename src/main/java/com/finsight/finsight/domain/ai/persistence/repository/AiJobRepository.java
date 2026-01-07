package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJobEntity, Long> {

    @Query("select j from AiJobEntity j join fetch j.article where j.id = :id")
    Optional<AiJobEntity> findByIdWithArticle(@Param("id") Long id);

    @Query(value = """
    SELECT ai_job_id
    FROM ai_jobs
    WHERE ai_job_id IN (
        SELECT ai_job_id
        FROM ai_jobs
        WHERE status = 'PENDING'
          AND job_type = :type
        ORDER BY requested_at ASC
        FETCH FIRST :limit ROWS ONLY
    )
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Long> findPendingIdsForUpdateSkipLocked(@Param("type") String type,
                                                 @Param("limit") int limit);

    @Modifying
    @Query(value = """
        UPDATE ai_jobs
        SET status = 'RUNNING',
            started_at = LOCALTIMESTAMP
        WHERE ai_job_id IN (:ids)
          AND status = 'PENDING'
        """, nativeQuery = true)
    int markRunning(@Param("ids") List<Long> ids);
}
