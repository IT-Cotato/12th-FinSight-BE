package com.finsight.finsight.domain.naver.presentation;

import com.finsight.finsight.domain.naver.application.dto.response.NaverArticleDetailResponse;
import com.finsight.finsight.domain.naver.domain.service.NaverArticleQueryService;
import com.finsight.finsight.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/naver/articles")
public class NaverArticleQueryController {

    private final NaverArticleQueryService naverArticleQueryService;

    @Operation(
            summary = "크롤링된 뉴스 단건 조회 // 테스트",
            description = "기사 메타 정보(본문 제외)를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기사 없음")
    })
    @GetMapping("/{articleId}")
    public ResponseEntity<DataResponse<NaverArticleDetailResponse>> detail(@PathVariable Long articleId) {
        return ResponseEntity.ok(
                DataResponse.from(naverArticleQueryService.detail(articleId))
        );
    }
}
