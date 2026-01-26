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

    @Schema(name = "HomePersonalizedNewsResponse", description = "맞춤 뉴스 응답")
    @Builder
    public record PersonalizedNewsResponse(
            @Schema(description = "조회 카테고리 (null이면 종합)")
            NaverEconomySection category,
            @Schema(description = "뉴스 목록 (최대 8개)")
            List<PersonalizedNewsItem> news) {
    }

    @Schema(name = "HomePersonalizedNewsItem", description = "맞춤 뉴스 아이템")
    @Builder
    public record PersonalizedNewsItem(
            @Schema(description = "기사 ID")
            Long newsId,
            @Schema(description = "카테고리")
            NaverEconomySection category,
            @Schema(description = "용어 3개")
            List<TermItem> terms,
            @Schema(description = "기사 제목")
            String title,
            @Schema(description = "썸네일 URL")
            String thumbnailUrl) {
    }

    @Schema(name = "HomeTermItem", description = "용어 아이템")
    @Builder
    public record TermItem(
            @Schema(description = "용어 ID")
            Long termId,
            @Schema(description = "용어명")
            String displayName) {
    }
}
