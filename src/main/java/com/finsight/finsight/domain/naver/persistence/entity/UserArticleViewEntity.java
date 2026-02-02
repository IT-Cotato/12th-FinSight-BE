package com.finsight.finsight.domain.naver.persistence.entity;

import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 뉴스 조회 기록 엔티티
 * 사용자가 읽은 뉴스를 추적하여 학습 통계에 활용
 */
@Entity
@Getter
@Table(name = "user_article_view", indexes = {
        @Index(name = "idx_user_article", columnList = "user_id, naver_article_id"),
        @Index(name = "idx_user_viewed", columnList = "user_id, viewed_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_user_article", columnNames = { "user_id", "naver_article_id" }))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserArticleViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naver_article_id", nullable = false)
    private NaverArticleEntity article;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public static UserArticleViewEntity create(UserEntity user, NaverArticleEntity article) {
        UserArticleViewEntity entity = new UserArticleViewEntity();
        entity.user = user;
        entity.article = article;
        entity.viewedAt = LocalDateTime.now();
        return entity;
    }
}
