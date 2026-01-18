package com.finsight.finsight.domain.ai.persistence.repository;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobStatus;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    Optional<AiJobEntity> findTopByArticle_IdAndJobTypeOrderByRequestedAtDesc(Long articleId, AiJobType jobType);

    /**
     * RUNNING 상태에서 stuck된 Job 조회 (runningStartedAt < threshold)
     */
    @Query("SELECT j FROM AiJobEntity j WHERE j.status = :status AND j.runningStartedAt < :threshold")
    List<AiJobEntity> findStuckRunningJobs(@Param("status") AiJobStatus status,
                                           @Param("threshold") LocalDateTime threshold);

    /**
     * RETRY_WAIT 상태에서 nextRunAt이 지난 Job 조회
     */
    @Query("SELECT j FROM AiJobEntity j WHERE j.status = :status AND j.nextRunAt <= :now")
    List<AiJobEntity> findRetryWaitJobsReadyToRun(@Param("status") AiJobStatus status,
                                                   @Param("now") LocalDateTime now);
}
