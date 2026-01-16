package com.finsight.finsight.domain.learning.persistence.mapper;

import com.finsight.finsight.domain.learning.application.dto.response.LearningResponseDTO;
import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiArticleSummaryEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiArticleInsightEntity;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LearningConverter {

    private final ObjectMapper objectMapper;

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
                                                .description(card.getTerm().getDefinition())
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

    private static String getSafe(List<String> list, int index) {
        if (list != null && index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return "";
    }

    // NewsDetailResponseDTO(상세 조회)
    public LearningResponseDTO.NewsDetailResponse toNewsDetailResponse(
            NaverArticleEntity article,
            List<AiTermCardEntity> terms,
            AiArticleSummaryEntity summary,
            AiArticleInsightEntity insight) {

        return LearningResponseDTO.NewsDetailResponse.builder()
                .category(article.getSection())
                .coreTerms(terms.stream()
                        .map(card -> LearningResponseDTO.CoreTerm.builder()
                                .termId(card.getTerm().getId())
                                .term(card.getTerm().getDisplayName())
                                .description(card.getTerm().getDefinition())
                                .build())
                        .toList())
                .title(article.getTitle())
                .date(article.getPublishedAt().toString())
                .summary3Lines(parseSummary3Lines(summary.getSummary3Lines()))
                .bodySummary(summary.getSummaryFull())
                .insights(parseInsights(insight.getInsightJson()))
                .build();
    }

    private LearningResponseDTO.Summary3Lines parseSummary3Lines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return LearningResponseDTO.Summary3Lines.builder().build();
        }

        try {
            String[] split = rawText.split("\n");
            List<String> lines = Arrays.stream(split)
                    .map(String::trim)
                    .toList();

            return LearningResponseDTO.Summary3Lines.builder()
                    .event(getSafe(lines, 0))
                    .reason(getSafe(lines, 1))
                    .impact(getSafe(lines, 2))
                    .build();
        } catch (Exception e) {
            return LearningResponseDTO.Summary3Lines.builder().build();
        }
    }

    private List<LearningResponseDTO.Insight> parseInsights(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode insightsNode = root.get("insights");
            if (insightsNode == null || !insightsNode.isArray()) {
                return Collections.emptyList();
            }

            List<LearningResponseDTO.Insight> result = new ArrayList<>();
            for (JsonNode node : insightsNode) {
                result.add(LearningResponseDTO.Insight.builder()
                        .title(node.path("title").asText(""))
                        .detail(node.path("detail").asText(""))
                        .whyItMatters(node.path("whyItMatters").asText(""))
                        .build());
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
