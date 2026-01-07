package com.finsight.finsight.domain.ai.domain.worker;

import com.finsight.finsight.domain.ai.domain.service.AiJobService;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
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
            log.error("[AI-WORKER] unexpected error", e);
        } finally {
            sample.stop(Timer.builder("ai_worker_run_seconds")
                    .publishPercentiles(0.5, 0.9, 0.99)
                    .register(meterRegistry));
            running.set(false);
        }
    }

    private void processType(AiJobType type) {
        List<Long> jobIds = aiJobService.claimNextJobIds(type, props.getBatchSize());
        if (jobIds.isEmpty()) {
            incClaim(type, "empty");
            return;
        }

        incClaim(type, "claimed", jobIds.size());
        log.info("[AI-WORKER] claimed type={} count={}", type, jobIds.size());

        for (Long jobId : jobIds) {
            long startNs = System.nanoTime();
            try {
                switch (type) {
                    case SUMMARY -> aiJobService.processSummary(jobId);
                    case TERM_CARDS -> aiJobService.processTermCards(jobId);
                    case INSIGHT -> aiJobService.processInsight(jobId);
                    case QUIZ_CONTENT -> aiJobService.processQuizContent(jobId);
                    case QUIZ_TERM -> aiJobService.processQuizTerm(jobId);
                }
                incExecute(type, "ok");
            } catch (Exception e) {
                incExecute(type, "error");
                log.error("[AI-WORKER] execute fail type={} jobId={} err={}", type, jobId, e.toString());
            } finally {
                long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                Timer.builder("ai_worker_job_seconds")
                        .tag("type", type.name())
                        .register(meterRegistry)
                        .record(tookMs, TimeUnit.MILLISECONDS);
            }
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

    private void incExecute(AiJobType type, String status) {
        Counter.builder("ai_worker_execute_total")
                .tag("type", type.name())
                .tag("status", status)
                .register(meterRegistry)
                .increment();
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
