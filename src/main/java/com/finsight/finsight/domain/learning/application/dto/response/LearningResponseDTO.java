package com.finsight.finsight.domain.learning.application.dto.response;

import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class LearningResponseDTO {

        // NewsListResDTO
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
                        String term) {
        }
}
