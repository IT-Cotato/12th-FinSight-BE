package com.finsight.finsight.domain.home.presentation;

import com.finsight.finsight.domain.home.application.dto.response.HomeResponseDTO;
import com.finsight.finsight.domain.home.domain.service.HomeNewsService;
import com.finsight.finsight.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeNewsService homeNewsService;

    @Operation(
            summary = "실시간 인기 뉴스 조회",
            description = "8개 카테고리별 인기순(조회수)으로 라운드 로빈 방식 반환. "
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인기 뉴스 조회 성공")
    })
    @GetMapping("/news/popular")
    public ResponseEntity<DataResponse<HomeResponseDTO.PopularNewsResponse>> getPopularNews(
            @Parameter(description = "한 페이지당 조회 개수") @RequestParam(defaultValue = "40") int size,
            @Parameter(description = "커서 (다음 페이지 조회를 위한 Base64 문자열)") @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(
                DataResponse.from(homeNewsService.getPopularNewsByCategory(size, cursor))
        );
    }
}
