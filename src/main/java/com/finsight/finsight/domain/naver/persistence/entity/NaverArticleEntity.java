package com.finsight.finsight.domain.naver.persistence.entity;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "naver_article",
        uniqueConstraints = @UniqueConstraint(name = "uk_naver_oid_aid", columnNames = {"oid", "aid"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NaverArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "naver_article_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "section", nullable = false, length = 30)
    private NaverEconomySection section;

    @Column(name = "oid", nullable = false, length = 10)
    private String oid;

    @Column(name = "aid", nullable = false, length = 30)
    private String aid;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "title", length = 2000)
    private String title;

    @Column(name = "press", length = 200)
    private String press;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Builder
    public NaverArticleEntity(NaverEconomySection section, String oid, String aid, String url,
                              String title, String press, LocalDateTime publishedAt,
                              String content, LocalDateTime collectedAt, String thumbnailUrl) {
        this.section = section;
        this.oid = oid;
        this.aid = aid;
        this.url = url;
        this.title = title;
        this.press = press;
        this.publishedAt = publishedAt;
        this.content = content;
        this.collectedAt = collectedAt;
        this.thumbnailUrl = thumbnailUrl;
    }
}

