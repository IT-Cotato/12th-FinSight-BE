package com.finsight.finsight.domain.category.application.dto.response;

import java.util.List;

public record CategoryOrderResponse(
        List<CategoryOrderItem> categories
) {
    public record CategoryOrderItem(
            Long categoryId,
            String code,
            String nameKo,
            Integer sortOrder
    ) {}
}
