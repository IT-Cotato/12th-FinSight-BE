package com.finsight.finsight.domain.naver.presentation;

import com.finsight.finsight.domain.naver.application.dto.response.NaverCrawlResultResponse;
import com.finsight.finsight.domain.naver.domain.service.NaverCrawlerService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.response.DefaultIdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/naver/economy")
public class NaverCrawlerController {

    private final NaverCrawlerService crawlerService;

    @Operation(
            summary = "네이버 경제(8개 탭) 크롤링 수동 실행 // 테스트",
            description = "네이버 뉴스 경제 8개 탭에서 최신 기사를 수집하고 DB에 저장합니다. (중복은 oid+aid로 방지)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "실패")
    })
    @PostMapping("/crawl")
    public ResponseEntity<DataResponse<NaverCrawlResultResponse>> crawlOnce() {
        return ResponseEntity.ok(
                DataResponse.from(crawlerService.crawlAllOnce())
        );
    }
}
