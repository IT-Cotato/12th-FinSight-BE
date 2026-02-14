package com.finsight.finsight.domain.naver.application.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class NaverCrawlResultResponse {

    private final int totalScanned;
    private final int totalSaved;
    private final List<CategoryResult> categories;

    private NaverCrawlResultResponse(int totalScanned, int totalSaved, List<CategoryResult> categories) {
        this.totalScanned = totalScanned;
        this.totalSaved = totalSaved;
        this.categories = categories;
    }

    public static NaverCrawlResultResponse of(int totalScanned, int totalSaved, List<CategoryResult> categories) {
        return new NaverCrawlResultResponse(totalScanned, totalSaved, categories);
    }

    @Getter
    public static class CategoryResult {
        private final String category;
        private final int scanned;
        private final int saved;

        private CategoryResult(String category, int scanned, int saved) {
            this.category = category;
            this.scanned = scanned;
            this.saved = saved;
        }

        public static CategoryResult of(String category, int scanned, int saved) {
            return new CategoryResult(category, scanned, saved);
        }
    }
}

