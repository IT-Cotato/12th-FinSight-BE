package com.finsight.finsight.domain.category.presentation;

import com.finsight.finsight.domain.category.application.dto.request.SaveCategoryRequest;
import com.finsight.finsight.domain.category.application.dto.response.CategoryResponse;
import com.finsight.finsight.domain.category.domain.service.CategoryService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.response.ErrorResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "관심분야 카테고리 API")
public class InterestCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(
            summary = "관심분야 저장",
            description = """
                    사용자의 관심분야를 저장합니다.

                    **최소 3개 이상 선택해야 합니다.**

                    ### 선택 가능한 카테고리
                    | 코드 | 설명 |
                    |------|------|
                    | FINANCE | 금융 |
                    | STOCK | 증권 |
                    | INDUSTRY | 산업/재계 |
                    | SME | 중기/벤처 |
                    | REAL_ESTATE | 부동산 |
                    | GLOBAL | 글로벌 경제 |
                    | LIVING | 생활경제 |
                    | GENERAL | 경제 일반 |
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "관심분야 저장 성공",
                    content = @Content(schema = @Schema(implementation = DataResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "최소 선택 개수 미달",
                                            value = """
                                                    {
                                                        "status": 400,
                                                        "code": "CATEGORY-001",
                                                        "message": "3개 이상 선택해주세요."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "유효하지 않은 카테고리",
                                            value = """
                                                    {
                                                        "status": 400,
                                                        "code": "CATEGORY-002",
                                                        "message": "유효하지 않은 관심분야입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "관심분야 저장 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SaveCategoryRequest.class),
                    examples = @ExampleObject(
                            name = "요청 예시",
                            value = """
                                    {
                                        "sections": ["FINANCE", "STOCK", "REAL_ESTATE"]
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<DataResponse<Void>> saveCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveCategoryRequest request
    ) {
        categoryService.saveCategories(userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @GetMapping
    @Operation(
            summary = "관심분야 조회",
            description = """
                    사용자가 저장한 관심분야 목록을 조회합니다.

                    ### 응답 형식
                    각 카테고리는 section(코드)과 displayName(한글명)으로 반환됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "관심분야 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = DataResponse.class),
                            examples = @ExampleObject(
                                    name = "응답 예시",
                                    value = """
                                            {
                                                "status": 200,
                                                "data": {
                                                    "categories": [
                                                        {"section": "FINANCE", "displayName": "금융"},
                                                        {"section": "STOCK", "displayName": "증권"},
                                                        {"section": "REAL_ESTATE", "displayName": "부동산"}
                                                    ]
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<DataResponse<CategoryResponse>> getCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CategoryResponse response = categoryService.getCategories(userDetails.getUserId());
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
