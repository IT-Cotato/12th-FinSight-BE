package com.finsight.finsight.domain.category.presentation;

import com.finsight.finsight.domain.category.application.dto.request.SaveCategoryRequest;
import com.finsight.finsight.domain.category.application.dto.response.CategoryResponse;
import com.finsight.finsight.domain.category.domain.service.CategoryService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "카테고리 API")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "관심분야 저장", description = "사용자의 관심분야를 저장합니다. 최소 3개 이상 선택해야 합니다.")
    public ResponseEntity<DataResponse<Void>> saveCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveCategoryRequest request
    ) {
        categoryService.saveCategories(userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @GetMapping
    @Operation(summary = "관심분야 조회", description = "사용자의 관심분야를 조회합니다.")
    public ResponseEntity<DataResponse<CategoryResponse>> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CategoryResponse response = categoryService.getCategories(userDetails.getUserId());
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
