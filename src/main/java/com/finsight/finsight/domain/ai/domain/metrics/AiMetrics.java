package com.finsight.finsight.domain.ai.domain.metrics;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobStatus;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class AiMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> queueSize = new ConcurrentHashMap<>();

    public void incEnqueue(AiJobType type, String result) {
        // result: ok | dup | skipped
        Counter.builder("ai_jobs_enqueued_total")
                .tag("type", type.name())
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void incProcessed(AiJobType type, String result) {
        incProcessed(type, result, null);
    }

    public void incProcessed(AiJobType type, String result, String errorCode) {
        Counter.Builder builder = Counter.builder("ai_jobs_processed_total")
                .tag("type", type.name())
                .tag("result", result); // success|failed|retry_wait|suspended

        if (errorCode != null) {
            builder.tag("error_code", errorCode);
        }

        builder.register(meterRegistry).increment();
    }

    public void incEvent(AiJobType type, String event) {
        Counter.builder("ai_jobs_events_total")
                .tag("type", type.name())
                .tag("event", event) // claimed|duplicate_enqueue|...
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, AiJobType type) {
        sample.stop(Timer.builder("ai_job_duration_seconds")
                .tag("type", type.name())
                .register(meterRegistry));
    }

    public void setQueueSize(AiJobType type, AiJobStatus status, long value) {
        String key = type.name() + ":" + status.name();
        AtomicLong holder = queueSize.computeIfAbsent(key, k -> {
            AtomicLong v = new AtomicLong(0);
            Gauge.builder("ai_jobs_queue_size", v, AtomicLong::get)
                    .tag("type", type.name())
                    .tag("status", status.name())
                    .register(meterRegistry);
            return v;
        });
        holder.set(value);
    }
}
