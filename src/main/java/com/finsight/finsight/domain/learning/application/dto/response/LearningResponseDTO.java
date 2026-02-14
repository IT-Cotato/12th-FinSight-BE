package com.finsight.finsight.domain.learning.application.dto.response;

import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class LearningResponseDTO {

        // NewsListResDTO(커서 기반 무한 스크롤 방식)
        @Schema(name = "LearningNewListResponse")
        @Builder
        public record NewListResponse(
                        Category category,
                        SortType sort,
                        int size,
                        boolean hasNext,
                        String nextCursor,
                        List<NewsItem> news) {
        }

        // 2. 검색/번호 페이지네이션용 (오프셋 기반)
        @Schema(name = "LearningSearchNewsResponse")
        @Builder
        public record SearchNewsResponse(
                int currentPage,      // 현재 페이지 번호 (1부터 시작)
                int totalPages,       // 전체 페이지 수
                long totalElements,   // 전체 검색 결과 개수
                int size,             // 한 페이지당 보여줄 개수 (4개)
                boolean isFirst,      // 첫 페이지 여부
                boolean isLast,       // 마지막 페이지 여부
                List<NewsItem> news) { // 검색된 뉴스 리스트
        }

        @Schema(name = "LearningNewsItem")
        @Builder
        public record NewsItem(
                        Long newsId,
                        NaverEconomySection category,
                        String title,
                        String thumbnailUrl,
                        List<CoreTerm> coreTerms) {
        }

        @Schema(name = "LearningCoreTerm")
        @Builder
        public record CoreTerm(
                        Long termId,
                        String term,
                        String description) {
        }

        // NewsDetailResDTO
        @Schema(name = "LearningNewsDetailResponse")
        @Builder
        public record NewsDetailResponse(
                        NaverEconomySection category,
                        List<CoreTerm> coreTerms,
                        String title,
                        String date,
                        String thumbnailUrl,
                        String originalUrl,
                        Summary3Lines summary3Lines,
                        String bodySummary,
                        List<Insight> insights) {
        }

        @Schema(name = "LearningSummary3Lines")
        @Builder
        public record Summary3Lines(
                        String event,
                        String reason,
                        String impact) {
        }

        @Schema(name = "LearningInsight")
        @Builder
        public record Insight(
                        String title,
                        String detail,
                        String whyItMatters) {
        }
}
