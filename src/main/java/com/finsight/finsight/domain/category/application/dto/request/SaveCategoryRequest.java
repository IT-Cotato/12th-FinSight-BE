package com.finsight.finsight.domain.category.application.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveCategoryRequest(
        @NotEmpty(message = "관심분야를 선택해주세요.")
        List<String> sections
) {}
