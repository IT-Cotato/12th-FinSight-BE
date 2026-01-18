package com.finsight.finsight.domain.ai.domain.worker;

import com.finsight.finsight.domain.ai.domain.lock.AiJobLockService;
import com.finsight.finsight.domain.ai.domain.service.AiJobService;
import com.finsight.finsight.domain.ai.exception.code.AiErrorCode;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import com.finsight.finsight.global.exception.AppException;
import io.micrometer.core.instrument.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJobWorker {

    private final AiJobService aiJobService;
    private final AiJobLockService lockService;
    private final MeterRegistry meterRegistry;
    private final AiWorkerProperties props;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${ai.worker.cron:*/30 * * * * *}")
    public void runScheduled() {
        if (!props.isEnabled()) return;

        if (!running.compareAndSet(false, true)) {
            incWorker("skipped_already_running");
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            incWorker("run");

            // 처리 순서: SUMMARY -> TERM -> INSIGHT -> QUIZ_CONTENT -> QUIZ_TERM
            for (AiJobType type : props.getProcessOrder()) {
                processType(type);
            }
        } catch (Exception e) {
            incWorker("run_error");
            log.error("[AI-WORKER] event_type=ai_worker_error", e);
        } finally {
            sample.stop(Timer.builder("ai_worker_run_seconds")
                    .publishPercentiles(0.5, 0.9, 0.99)
                    .register(meterRegistry));
            running.set(false);
        }
    }

    private void processType(AiJobType type) {
        // 1. PENDING Job ID 목록 조회
        List<Long> jobIds = aiJobService.findPendingJobIds(type, props.getBatchSize());
        if (jobIds.isEmpty()) {
            incClaim(type, "empty");
            return;
        }

        log.info("[AI-WORKER] event_type=ai_worker_found type={} count={}", type, jobIds.size());

        int processed = 0;
        for (Long jobId : jobIds) {
            if (processJobWithLock(type, jobId)) {
                processed++;
            }
        }

        incClaim(type, "processed", processed);
    }

    /**
     * 단일 Job 처리 (락 획득 → RUNNING 전환 → 처리 → finally unlock)
     */
    private boolean processJobWithLock(AiJobType type, Long jobId) {
        // 2. Redis 락 획득 시도
        String lockToken = lockService.tryLock(jobId);
        if (lockToken == null) {
            incExecute(type, "lock_failed", null);
            log.debug("[AI-WORKER] event_type=ai_lock_failed job_id={}", jobId);
            return false;
        }

        long startNs = System.nanoTime();

        try {
            // 3. RUNNING 전환 (DB)
            if (!aiJobService.tryMarkRunning(jobId)) {
                incExecute(type, "already_running", null);
                log.debug("[AI-WORKER] event_type=ai_already_running job_id={}", jobId);
                return false;
            }

            log.info("[AI-WORKER] event_type=ai_job_start job_type={} job_id={}", type, jobId);

            // 4. 실제 처리
            switch (type) {
                case SUMMARY -> aiJobService.processSummary(jobId);
                case TERM_CARDS -> aiJobService.processTermCards(jobId);
                case INSIGHT -> aiJobService.processInsight(jobId);
                case QUIZ_CONTENT -> aiJobService.processQuizContent(jobId);
                case QUIZ_TERM -> aiJobService.processQuizTerm(jobId);
            }

            incExecute(type, "success", null);
            return true;

        } catch (AppException e) {
            // 에러 유형별 상태 전환은 AiJobService에서 처리됨
            String errorCode = e.getErrorCode() != null ? e.getErrorCode().getCode() : "UNKNOWN";
            incExecute(type, "error", errorCode);
            log.warn("[AI-WORKER] event_type=ai_job_error job_type={} job_id={} error_code={}",
                    type, jobId, errorCode);
            return false;

        } catch (Exception e) {
            // 예기치 못한 에러는 FAILED 처리
            incExecute(type, "error", "UNEXPECTED");
            log.error("[AI-WORKER] event_type=ai_job_unexpected job_type={} job_id={}", type, jobId, e);

            try {
                aiJobService.handleJobError(jobId, AiErrorCode.OPENAI_API_FAIL);
            } catch (Exception ignore) {
                // 에러 처리 실패는 무시
            }
            return false;

        } finally {
            // 5. 항상 unlock
            lockService.unlock(jobId, lockToken);

            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            Timer.builder("ai_worker_job_seconds")
                    .tag("type", type.name())
                    .register(meterRegistry)
                    .record(tookMs, TimeUnit.MILLISECONDS);
        }
    }

    private void incWorker(String event) {
        Counter.builder("ai_worker_events_total")
                .tag("event", event)
                .register(meterRegistry)
                .increment();
    }

    private void incClaim(AiJobType type, String status) {
        Counter.builder("ai_worker_claim_total")
                .tag("type", type.name())
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    private void incClaim(AiJobType type, String status, int n) {
        Counter.builder("ai_worker_claim_total")
                .tag("type", type.name())
                .tag("status", status)
                .register(meterRegistry)
                .increment(n);
    }

    private void incExecute(AiJobType type, String status, String errorCode) {
        Counter.Builder builder = Counter.builder("ai_worker_execute_total")
                .tag("type", type.name())
                .tag("status", status);

        if (errorCode != null) {
            builder.tag("error_code", errorCode);
        }

        builder.register(meterRegistry).increment();
    }

    @Component
    @ConfigurationProperties(prefix = "ai.worker")
    @Getter @Setter
    public static class AiWorkerProperties {
        private boolean enabled = true;
        private int batchSize = 5;
        private AiJobType[] processOrder = new AiJobType[]{
                AiJobType.SUMMARY,
                AiJobType.TERM_CARDS,
                AiJobType.INSIGHT,
                AiJobType.QUIZ_CONTENT,
                AiJobType.QUIZ_TERM
        };
    }
}
