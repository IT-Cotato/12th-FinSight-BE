package com.finsight.finsight.domain.home.presentation;

import com.finsight.finsight.domain.home.application.dto.response.HomeResponseDTO;
import com.finsight.finsight.domain.home.domain.service.HomeNewsService;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/home")
public class HomeController {

    private final HomeNewsService homeNewsService;

    @Operation(
            summary = "실시간 인기 뉴스 조회",
            description = "8개 카테고리별 인기순(조회수)으로 라운드 로빈 방식 반환. "
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인기 뉴스 조회 성공"),
            @ApiResponse(responseCode = "401", description = "접근 권한 없음(AUTH-011)")
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

    @Operation(
            summary = "맞춤 뉴스 조회",
            description = """
                    사용자 맞춤 뉴스를 조회합니다.

                    **종합 조회 (category 미지정)**
                    - 기사 8개 반환

                    **특정 카테고리 조회**:
                    - 해당 카테고리 기사 최신순 8개

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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "맞춤 뉴스 조회 성공"),
            @ApiResponse(responseCode = "401", description = "접근 권한 없음(AUTH-011)"),
            @ApiResponse(responseCode = "400", description = "사용자 관심 카테고리가 아님(HOME-001)"),
    })
    @GetMapping("/news/personalized")
    public ResponseEntity<DataResponse<HomeResponseDTO.PersonalizedNewsResponse>> getPersonalizedNews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "카테고리 (미지정 시 종합)") @RequestParam(required = false) NaverEconomySection category
    ) {
        return ResponseEntity.ok(
                DataResponse.from(homeNewsService.getPersonalizedNews(userDetails.getUserId(), category))
        );
    }

    @Operation(
            summary = "홈 상태 메시지 조회",
            description = "사용자의 최근 뉴스 보관 및 퀴즈 풀이 현황에 따른 메시지를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홈 상태 메시지 조회 성공"),
            @ApiResponse(responseCode = "401", description = "접근 권한 없음(AUTH-011)")
    })
    @GetMapping("/status")
    public ResponseEntity<DataResponse<HomeResponseDTO.HomeStatusResponse>> getHomeStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                DataResponse.from(homeNewsService.getHomeStatus(userDetails.getUserId()))
        );
    }

    @Operation(
            summary = "일일 체크리스트 조회",
            description = """
                    오늘의 일일 미션 달성 여부를 조회합니다.
                    1. 뉴스 1개 저장하기
                    2. 퀴즈 1개 풀기 (신규)
                    3. 보관한 퀴즈 복습하기 (기존 기록 업데이트)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크리스트 조회 성공"),
            @ApiResponse(responseCode = "401", description = "접근 권한 없음(AUTH-011)")
    })
    @GetMapping("/checklist")
    public ResponseEntity<DataResponse<HomeResponseDTO.DailyChecklistResponse>> getDailyChecklist(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                DataResponse.from(homeNewsService.getDailyChecklist(userDetails.getUserId()))
        );
    }
}
