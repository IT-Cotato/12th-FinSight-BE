package com.finsight.finsight.domain.category.application.dto.response;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;

import java.util.List;

public record CategoryResponse(
        List<CategoryItem> categories
) {
    public record CategoryItem(
            String section,
            String displayName
    ) {
        public static CategoryItem from(NaverEconomySection section) {
            return new CategoryItem(section.name(), section.getDisplayName());
        }
    }

    public static CategoryResponse from(List<NaverEconomySection> sections) {
        List<CategoryItem> items = sections.stream()
                .map(CategoryItem::from)
                .toList();
        return new CategoryResponse(items);
    }
}
