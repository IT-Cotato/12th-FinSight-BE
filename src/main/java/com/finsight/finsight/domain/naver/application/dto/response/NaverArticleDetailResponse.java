package com.finsight.finsight.domain.naver.application.dto.response;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;

import java.time.LocalDateTime;

public record NaverArticleDetailResponse(
        Long id,
        String category,
        String url,
        String title,
        String press,
        LocalDateTime publishedAt,
        LocalDateTime collectedAt,
        String thumbnailUrl
) {
    public static NaverArticleDetailResponse from(NaverArticleEntity a) {
        return new NaverArticleDetailResponse(
                a.getId(),
                a.getCategory(),
                a.getUrl(),
                a.getTitle(),
                a.getPress(),
                a.getPublishedAt(),
                a.getCollectedAt(),
                a.getThumbnailUrl()
        );
    }
}
