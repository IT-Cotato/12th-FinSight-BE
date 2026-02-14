package com.finsight.finsight.domain.ai.presentation;

import com.finsight.finsight.domain.ai.application.dto.response.ArticleAiResultResponse;
import com.finsight.finsight.domain.ai.domain.service.ArticleAiQueryService;
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
public class ArticleAiQueryController {

    private final ArticleAiQueryService articleAiQueryService;

    @Operation(
            summary = "기사 AI 결과 조회(모든 AI 생성 완료 시에만) // 테스트",
            description = "summary/terms/insight/quiz 생성이 모두 완료된 기사에 대해서만 AI 결과를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "AI 결과 미완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기사 없음")
    })
    @GetMapping("/{articleId}/ai")
    public ResponseEntity<DataResponse<ArticleAiResultResponse>> getAiResult(@PathVariable Long articleId) {
        return ResponseEntity.ok(
                DataResponse.from(articleAiQueryService.getAiResultRequireAll(articleId))
        );
    }
}

