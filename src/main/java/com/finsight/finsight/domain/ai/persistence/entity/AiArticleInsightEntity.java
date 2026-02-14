package com.finsight.finsight.domain.ai.persistence.entity;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "ai_article_insight",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_insight_job", columnNames = {"ai_job_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiArticleInsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_article_insight_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_job_id", nullable = false)
    private AiJobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_article_id", nullable = false)
    private NaverArticleEntity article;

    @Lob
    @Column(name = "insight_json", nullable = false)
    private String insightJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiArticleInsightEntity(AiJobEntity job, NaverArticleEntity article, String insightJson) {
        this.job = job;
        this.article = article;
        this.insightJson = insightJson;
        this.createdAt = LocalDateTime.now();
    }
}
