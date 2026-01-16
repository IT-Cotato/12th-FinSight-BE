package com.finsight.finsight.domain.learning.presentation;

import com.finsight.finsight.domain.learning.application.dto.response.LearningResponseDTO;
import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.learning.domain.service.NewsQueryService;
import com.finsight.finsight.domain.naver.domain.service.ArticleViewService;
import com.finsight.finsight.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("api/v1/news")
public class LearningController {

    private final NewsQueryService newsQueryService;
    private final ArticleViewService articleViewService;

    @Operation(summary = "뉴스 목록 조회 (커서 기반 페이지네이션 API)", description = "카테고리별, 정렬 조건별 뉴스 목록을 커서 기반 페이지네이션으로 조회합니다. (종합 외 카테고리는 최신순만 지원)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기사 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<DataResponse<LearningResponseDTO.NewListResponse>> getNewsList(
            @Parameter(description = "카테고리 (ALL, FINANCE, STOCK, ...)") @RequestParam(defaultValue = "ALL") Category category,

            @Parameter(description = "정렬 방식 (LATEST, POPULARITY)") @RequestParam(defaultValue = "LATEST") SortType sort,

            @Parameter(description = "한 페이지당 조회 개수") @RequestParam(defaultValue = "40") int size,

            @Parameter(description = "커서 (다음 페이지 조회를 위한 Base64 문자열)") @RequestParam(required = false) String cursor) {
        // 카테고리가 all이 아닌 경우는 최신순만 지원
        SortType finalSort = (category != Category.ALL) ? SortType.LATEST : sort;

        return ResponseEntity.ok(
                DataResponse.from(newsQueryService.getNewsList(category, finalSort, size, cursor)));
    }

    @Operation(summary = "상세 뉴스 조회 API", description = "뉴스를 클릭했을 때 조회수를 증가시키고 상세 뉴스(뉴스 카테고리, 핵심 용어 3개, 제목/날짜(시간), 3줄 요약, 본문 요약(전체 요약), 인사이트)를 제공합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 뉴스 조회 성공")
    })
    @GetMapping("/{newsId}")
    public ResponseEntity<DataResponse<LearningResponseDTO.NewsDetailResponse>> getNews(@PathVariable Long newsId) {
        // 조회수 증가
        articleViewService.incrementViewCount(newsId);

        return ResponseEntity.ok(
                DataResponse.from(newsQueryService.getNewsDetails(newsId)));
    }

    @Operation(summary = "뉴스 검색 API", description = "뉴스 제목, 용어, 내용 요약, 3줄 요약에서 검색어를 통한 검색한 뉴스 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 뉴스 조회 성공")
    })
    @GetMapping("/search")
    public ResponseEntity<DataResponse<LearningResponseDTO.NewListResponse>> searchNewsList(
            @RequestParam(name = "q") String query, // 한 글자 이상의 검색어
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "4") int size
    ){
        return ResponseEntity.ok(
                DataResponse.from(newsQueryService.searchNews(query, SortType.LATEST, size, cursor)));
    }
}