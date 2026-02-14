package com.finsight.finsight.domain.ai.persistence.entity;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "ai_quiz_set",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_quiz_job", columnNames = {"ai_job_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuizSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_quiz_set_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_job_id", nullable = false)
    private AiJobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_article_id", nullable = false)
    private NaverArticleEntity article;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_kind", nullable = false, length = 30)
    private AiJobType quizKind; // QUIZ_CONTENT or QUIZ_TERM

    @Lob
    @Column(name = "quiz_json", nullable = false)
    private String quizJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiQuizSetEntity(AiJobEntity job, NaverArticleEntity article, AiJobType quizKind, String quizJson) {
        this.job = job;
        this.article = article;
        this.quizKind = quizKind;
        this.quizJson = quizJson;
        this.createdAt = LocalDateTime.now();
    }
}
