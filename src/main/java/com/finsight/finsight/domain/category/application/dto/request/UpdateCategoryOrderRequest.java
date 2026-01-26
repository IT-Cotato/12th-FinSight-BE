package com.finsight.finsight.domain.category.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateCategoryOrderRequest(
        @NotEmpty(message = "카테고리 순서를 입력해주세요.")
        @Valid
        List<CategoryOrderItem> orders
) {
    public record CategoryOrderItem(
            @NotNull(message = "카테고리 ID를 입력해주세요.")
            Long categoryId,

            @NotNull(message = "순서를 입력해주세요.")
            Integer sortOrder
    ) {}
}
