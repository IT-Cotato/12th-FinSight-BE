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
        this.startedAt = LocalDateTime.now();
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
}

