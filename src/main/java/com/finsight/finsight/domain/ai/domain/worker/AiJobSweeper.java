package com.finsight.finsight.domain.ai.domain.worker;

import com.finsight.finsight.domain.ai.domain.metrics.AiMetrics;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobStatus;
import com.finsight.finsight.domain.ai.persistence.repository.AiJobRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI Job Sweeper - 중단 복구 스케줄러
 *
 * 1분 주기로 실행하며 다음 작업 수행:
 * - RUNNING 상태에서 stuck된 Job 복구 (10분 이상 RUNNING)
 * - RETRY_WAIT 상태에서 nextRunAt이 지난 Job을 PENDING으로 전환
 */
@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJobSweeper {

    private final AiJobRepository aiJobRepository;
    private final AiMetrics aiMetrics;
    private final AiSweeperProperties props;

    @Scheduled(cron = "${ai.sweeper.cron:0 */1 * * * *}")
    @Transactional
    public void sweep() {
        if (!props.isEnabled()) {
            return;
        }

        log.debug("[AI-SWEEPER] event_type=ai_sweeper_start");
        aiMetrics.incSweeperEvent("run");

        try {
            int stuckRecovered = recoverStuckJobs();
            int retryWaitRecovered = recoverRetryWaitJobs();

            if (stuckRecovered > 0 || retryWaitRecovered > 0) {
                log.info("[AI-SWEEPER] event_type=ai_sweeper_complete stuck_recovered={} retry_wait_recovered={}",
                        stuckRecovered, retryWaitRecovered);
            }
        } catch (Exception e) {
            aiMetrics.incSweeperEvent("error");
            log.error("[AI-SWEEPER] event_type=ai_sweeper_error", e);
        }
    }

    /**
     * RUNNING 상태에서 stuck된 Job 복구
     * - runningStartedAt 기준으로 stuckThresholdMinutes(기본 10분) 초과면 stuck
     * - retryCount < maxRetries: RETRY_WAIT로 전환 (1분 후 재시도)
     * - retryCount >= maxRetries: FAILED로 전환
     */
    private int recoverStuckJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(props.getStuckThresholdMinutes());
        List<AiJobEntity> stuckJobs = aiJobRepository.findStuckRunningJobs(AiJobStatus.RUNNING, threshold);

        if (stuckJobs.isEmpty()) {
            return 0;
        }

        int recoveredToRetry = 0;
        int recoveredToFailed = 0;

        for (AiJobEntity job : stuckJobs) {
            if (job.canRetry()) {
                job.markStuckRetryWait();
                recoveredToRetry++;
                log.warn("[AI-SWEEPER] event_type=ai_job_stuck_recovered job_id={} job_type={} retry_count={} action=RETRY_WAIT",
                        job.getId(), job.getJobType(), job.getRetryCount());
            } else {
                job.markStuckFailed();
                recoveredToFailed++;
                log.warn("[AI-SWEEPER] event_type=ai_job_stuck_failed job_id={} job_type={} action=FAILED",
                        job.getId(), job.getJobType());
            }
        }

        if (recoveredToRetry > 0) {
            aiMetrics.incSweeperRecovered("stuck_to_retry", recoveredToRetry);
        }
        if (recoveredToFailed > 0) {
            aiMetrics.incSweeperRecovered("stuck_to_failed", recoveredToFailed);
        }

        return recoveredToRetry + recoveredToFailed;
    }

    /**
     * RETRY_WAIT 상태에서 nextRunAt이 지난 Job을 PENDING으로 전환
     */
    private int recoverRetryWaitJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<AiJobEntity> retryWaitJobs = aiJobRepository.findRetryWaitJobsReadyToRun(AiJobStatus.RETRY_WAIT, now);

        if (retryWaitJobs.isEmpty()) {
            return 0;
        }

        for (AiJobEntity job : retryWaitJobs) {
            job.markPendingForRetry();
            log.info("[AI-SWEEPER] event_type=ai_job_retry_wait_to_pending job_id={} job_type={} retry_count={}",
                    job.getId(), job.getJobType(), job.getRetryCount());
        }

        aiMetrics.incSweeperRecovered("retry_wait_to_pending", retryWaitJobs.size());
        return retryWaitJobs.size();
    }

    @Component
    @ConfigurationProperties(prefix = "ai.sweeper")
    @Getter
    @Setter
    public static class AiSweeperProperties {
        private boolean enabled = true;
        private int stuckThresholdMinutes = 10;
    }
}
