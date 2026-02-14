package com.finsight.finsight.domain.learning.persistence.mapper;

import com.finsight.finsight.domain.learning.application.dto.response.LearningResponseDTO;
import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiArticleSummaryEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiArticleInsightEntity;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LearningConverter {

    private final ObjectMapper objectMapper;

    // 한국어 날짜 포맷터: "2026.01.01. 오후 4:51"
    private static final DateTimeFormatter KOREAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd. a h:mm",
            Locale.KOREAN);

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

    // 엔티티 -> DTO
    public static LearningResponseDTO.SearchNewsResponse toSearchNewsResponse(
            List<NaverArticleEntity> content,
            Map<Long, List<AiTermCardEntity>> articleTermMap,
            long totalElements,
            int totalPages,
            int page,
            int size) {
        // 1. 엔티티 리스트를 NewsItem DTO 리스트로 변환
        List<LearningResponseDTO.NewsItem> newsItems = content.stream()
                .map(article -> toNewsItem(article, articleTermMap.getOrDefault(article.getId(), List.of())))
                .toList();

        // 2. SearchNewsResponse 생성 (번호 기반 페이지네이션 정보 포함)
        return LearningResponseDTO.SearchNewsResponse.builder()
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .size(size)
                .isFirst(page == 0) // 현재 페이지가 0이면 첫 페이지
                .isLast(page >= totalPages - 1) // 현재 페이지가 전체 페이지-1 이상이면 마지막 페이지
                .news(newsItems)
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
                .date(formatKoreanDate(article.getPublishedAt()))
                .thumbnailUrl(article.getThumbnailUrl())
                .originalUrl(article.getUrl())
                .summary3Lines(parseSummary3Lines(summary.getSummary3Lines()))
                .bodySummary(summary.getSummaryFull())
                .insights(parseInsights(insight.getInsightJson()))
                .build();
    }

    // LocalDateTime을 한국어 형식 날짜로 변환
    private String formatKoreanDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(KOREAN_DATE_FORMATTER);
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
