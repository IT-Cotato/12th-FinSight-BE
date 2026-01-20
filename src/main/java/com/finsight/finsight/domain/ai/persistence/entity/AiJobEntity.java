package com.finsight.finsight.domain.ai.persistence.entity;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "ai_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_job_article_type_version",
                columnNames = {"naver_article_id", "job_type", "prompt_version"}
        ),
        indexes = {
                @Index(name = "idx_ai_job_status", columnList = "status"),
                @Index(name = "idx_ai_job_article", columnList = "naver_article_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_job_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_article_id", nullable = false)
    private NaverArticleEntity article;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private AiJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiJobStatus status;

    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion; // 예: v1, v2

    @Column(name = "model", length = 50)
    private String model; // 예: gpt-4.1-mini 등 (선택)

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "running_started_at")
    private LocalDateTime runningStartedAt;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Lob
    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Builder
    private AiJobEntity(NaverArticleEntity article,
                        AiJobType jobType,
                        AiJobStatus status,
                        String promptVersion,
                        String model,
                        int retryCount,
                        Integer maxRetries,
                        LocalDateTime nextRunAt,
                        LocalDateTime runningStartedAt,
                        String lastErrorCode,
                        String lastErrorMessage,
                        LocalDateTime requestedAt,
                        LocalDateTime startedAt,
                        LocalDateTime finishedAt) {
        this.article = article;
        this.jobType = jobType;
        this.status = status;
        this.promptVersion = promptVersion;
        this.model = model;
        this.retryCount = retryCount;
        this.maxRetries = (maxRetries != null) ? maxRetries : 3;
        this.nextRunAt = nextRunAt;
        this.runningStartedAt = runningStartedAt;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.requestedAt = requestedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public static AiJobEntity pending(NaverArticleEntity article, AiJobType type, String promptVersion, String model) {
        return AiJobEntity.builder()
                .article(article)
                .jobType(type)
                .status(AiJobStatus.PENDING)
                .promptVersion(promptVersion)
                .model(model)
                .retryCount(0)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public void markRunning() {
        this.status = AiJobStatus.RUNNING;
        this.runningStartedAt = LocalDateTime.now();
        if (this.startedAt == null) {
            this.startedAt = this.runningStartedAt;
        }
    }

    public void markSuccess() {
        this.status = AiJobStatus.SUCCESS;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = AiJobStatus.FAILED;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.retryCount++;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * RETRY_WAIT 상태로 전환 (재시도 가능 에러: 429, 5xx, timeout)
     * - retryCount 증가
     * - nextRunAt 설정 (지수 백오프)
     */
    public void markRetryWait(String errorCode, String errorMessage) {
        this.status = AiJobStatus.RETRY_WAIT;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.retryCount++;
        this.nextRunAt = calculateNextRunAt();
        this.runningStartedAt = null;
    }

    /**
     * SUSPENDED 상태로 전환 (재시도 불가: 402, insufficient_quota, 401, 403)
     * - 수동 확인 필요
     */
    public void markSuspended(String errorCode, String errorMessage) {
        this.status = AiJobStatus.SUSPENDED;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
        this.runningStartedAt = null;
    }

    /**
     * RETRY_WAIT → PENDING 전환 (스위퍼가 호출)
     */
    public void markPendingForRetry() {
        if (this.status != AiJobStatus.RETRY_WAIT) {
            throw new IllegalStateException("Cannot mark pending: current status is " + this.status);
        }
        this.status = AiJobStatus.PENDING;
        this.nextRunAt = null;
        this.runningStartedAt = null;
    }

    /**
     * 재시도 가능 여부
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    /**
     * RUNNING stuck → RETRY_WAIT 전환 (스위퍼가 호출)
     * - 재시도 가능한 경우: RETRY_WAIT 상태로 전환
     */
    public void markStuckRetryWait() {
        if (this.status != AiJobStatus.RUNNING) {
            throw new IllegalStateException("Cannot mark stuck: current status is " + this.status);
        }
        this.status = AiJobStatus.RETRY_WAIT;
        this.lastErrorCode = "STUCK_TIMEOUT";
        this.lastErrorMessage = "Job stuck in RUNNING state, recovered by sweeper";
        this.retryCount++;
        this.nextRunAt = LocalDateTime.now().plusMinutes(1); // 1분 후 재시도
        this.runningStartedAt = null;
    }

    /**
     * RUNNING stuck → FAILED 전환 (스위퍼가 호출)
     * - 재시도 불가능한 경우: FAILED 상태로 전환
     */
    public void markStuckFailed() {
        if (this.status != AiJobStatus.RUNNING) {
            throw new IllegalStateException("Cannot mark stuck failed: current status is " + this.status);
        }
        this.status = AiJobStatus.FAILED;
        this.lastErrorCode = "STUCK_TIMEOUT";
        this.lastErrorMessage = "Job stuck in RUNNING state, max retries exceeded";
        this.finishedAt = LocalDateTime.now();
        this.runningStartedAt = null;
    }

    /**
     * RUNNING 상태가 stuck 상태인지 확인
     * @param stuckThresholdMinutes stuck 판정 기준 (분)
     */
    public boolean isStuck(int stuckThresholdMinutes) {
        if (this.status != AiJobStatus.RUNNING || this.runningStartedAt == null) {
            return false;
        }
        return this.runningStartedAt.plusMinutes(stuckThresholdMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * RETRY_WAIT 상태가 재시도 가능 시점인지 확인
     */
    public boolean isReadyForRetry() {
        if (this.status != AiJobStatus.RETRY_WAIT || this.nextRunAt == null) {
            return false;
        }
        return !this.nextRunAt.isAfter(LocalDateTime.now());
    }

    /**
     * SUSPENDED → PENDING 전환 (관리자 resume)
     * - retryCount 초기화
     * - 에러 정보 유지 (이력 추적용)
     */
    public void resume() {
        if (this.status != AiJobStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot resume: current status is " + this.status);
        }
        this.status = AiJobStatus.PENDING;
        this.retryCount = 0;
        this.finishedAt = null;
        this.nextRunAt = null;
        this.runningStartedAt = null;
    }

    /**
     * 지수 백오프로 nextRunAt 계산
     * - 1차: 30초, 2차: 60초, 3차: 120초...
     */
    private LocalDateTime calculateNextRunAt() {
        long delaySeconds = 30L * (1L << Math.min(this.retryCount, 5));
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}

