package com.finsight.finsight.domain.learning.persistence.mapper;

import com.finsight.finsight.domain.learning.application.dto.response.LearningResponseDTO;
import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;

import java.util.List;

public class LearningConverter {

    // 단일 엔티티 -> NewsItem DTO
    public static LearningResponseDTO.NewsItem toNewsItem(NaverArticleEntity entity, List<AiTermCardEntity> terms) {
        return LearningResponseDTO.NewsItem.builder()
                .newsId(entity.getId())
                .category(entity.getSection())
                .title(entity.getTitle())
                .thumbnailUrl(entity.getThumbnailUrl())
                // 용어 매핑: AiTermCardEntity -> CoreTerm
                .coreTerms(
                        terms == null ? List.of()
                                : terms.stream()
                                        .map(card -> LearningResponseDTO.CoreTerm.builder()
                                                .termId(card.getTerm().getId())
                                                .term(card.getTerm().getDisplayName())
                                                .build())
                                        .toList())
                .build();
    }

    // 결과 리스트 -> NewListResponse DTO
    public static LearningResponseDTO.NewListResponse toNewListResponse(
            List<NaverArticleEntity> content,
            java.util.Map<Long, List<AiTermCardEntity>> articleTermMap,
            Category category,
            SortType sort,
            int size,
            boolean hasNext,
            String nextCursor) {
        return LearningResponseDTO.NewListResponse.builder()
                .category(category)
                .sort(sort)
                .size(size)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .news(content.stream()
                        .map(article -> toNewsItem(article, articleTermMap.getOrDefault(article.getId(), List.of())))
                        .toList())
                .build();
    }
}
