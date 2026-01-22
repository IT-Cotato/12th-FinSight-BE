package com.finsight.finsight.domain.home.application.dto.response;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class HomeResponseDTO {

    @Schema(name = "HomePopularNewsResponse")
    @Builder
    public record PopularNewsResponse(
            int size,
            boolean hasNext,
            String nextCursor,
            List<PopularNewsItem> news) {
    }

    @Schema(name = "HomePopularNewsItem")
    @Builder
    public record PopularNewsItem(
            Long newsId,
            NaverEconomySection category,
            String title,
            String thumbnailUrl) {
    }
}
