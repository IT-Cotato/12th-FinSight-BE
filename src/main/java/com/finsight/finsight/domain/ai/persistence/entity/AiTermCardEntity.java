package com.finsight.finsight.domain.ai.persistence.entity;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "ai_term_cards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_term_job_order",
                columnNames = {"ai_job_id", "card_order"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTermCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_term_card_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_job_id", nullable = false)
    private AiJobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_article_id", nullable = false)
    private NaverArticleEntity article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private TermEntity term; // 전역 용어 참조

    @Column(name = "card_order", nullable = false)
    private int cardOrder;

    @Lob
    @Column(name = "highlight_text")
    private String highlightText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiTermCardEntity(AiJobEntity job, NaverArticleEntity article, TermEntity term,
                             int cardOrder, String highlightText) {
        this.job = job;
        this.article = article;
        this.term = term;
        this.cardOrder = cardOrder;
        this.highlightText = highlightText;
        this.createdAt = LocalDateTime.now();
    }
}
