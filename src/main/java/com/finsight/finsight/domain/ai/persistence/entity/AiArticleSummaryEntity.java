package com.finsight.finsight.domain.ai.persistence.entity;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "ai_article_summary",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_summary_job", columnNames = {"ai_job_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiArticleSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_article_summary_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_job_id", nullable = false)
    private AiJobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_article_id", nullable = false)
    private NaverArticleEntity article;

    @Column(name = "summary_3_lines", length = 2000)
    private String summary3Lines;

    @Lob
    @Column(name = "summary_full")
    private String summaryFull;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiArticleSummaryEntity(AiJobEntity job, NaverArticleEntity article, String summary3Lines, String summaryFull) {
        this.job = job;
        this.article = article;
        this.summary3Lines = summary3Lines;
        this.summaryFull = summaryFull;
        this.createdAt = LocalDateTime.now();
    }
}

