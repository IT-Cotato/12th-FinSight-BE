package com.finsight.finsight.domain.mypage.presentation;

import com.finsight.finsight.domain.auth.application.dto.request.CheckNicknameRequest;
import com.finsight.finsight.domain.mypage.application.dto.request.UpdateNotificationRequest;
import com.finsight.finsight.domain.mypage.application.dto.request.UpdateProfileRequest;
import com.finsight.finsight.domain.mypage.application.dto.response.LearningReportResponse;
import com.finsight.finsight.domain.mypage.application.dto.response.MypageResponse;
import com.finsight.finsight.domain.mypage.application.dto.response.NotificationResponse;
import com.finsight.finsight.domain.mypage.domain.service.MypageService;
import com.finsight.finsight.domain.mypage.exception.MypageException;
import com.finsight.finsight.domain.mypage.exception.code.MypageErrorCode;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService myPageService;

    @Operation(summary = "유저 정보를 조회합니다.", description = "유저의 닉네임, 레벨 정보를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 정보 조회 성공")
    })
    @GetMapping("/me/profile")
    public ResponseEntity<DataResponse<MypageResponse.MemberProfileResponse>> getUserProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }

        Long userId = customUserDetails.getUserId();
        return ResponseEntity.ok(
                DataResponse.from(myPageService.getUserProfile(userId)));
    }

    @Operation(summary = "회원 탈퇴를 진행합니다.", description = "회원 정보를 영구적으로 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "회원 탈퇴 성공")
    })
    @DeleteMapping("/me")
    public ResponseEntity<DataResponse<Void>> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }
        Long userId = customUserDetails.getUserId();
        myPageService.withdrawMember(userId);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @Operation(summary = "닉네임 중복을 확인합니다.", description = "변경할 닉네임의 중복 여부를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 존재하는 닉네임")
    })
    @PostMapping("/check-nickname")
    public ResponseEntity<DataResponse<Void>> checkNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CheckNicknameRequest request
    ) {
        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }
        Long userId = customUserDetails.getUserId();
        myPageService.checkNickname(userId, request.nickname());
        return ResponseEntity.ok(DataResponse.ok());
    }

    @Operation(summary = "프로필을 수정합니다.", description = "닉네임과 관심 카테고리를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 수정 성공")
    })
    @PutMapping("/me/profile")
    public ResponseEntity<DataResponse<Void>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }
        Long userId = customUserDetails.getUserId();
        myPageService.updateProfile(userId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @Operation(summary = "학습 리포트를 조회합니다.", description = "누적 통계 및 주차별 학습 현황을 반환합니다. weeksAgo로 과거 주차 조회 가능 (0: 이번주, 1: 지난주, 최대 7주 전까지)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "학습 리포트 조회 성공")
    })
    @GetMapping("/report")
    public ResponseEntity<DataResponse<LearningReportResponse.Report>> getLearningReport(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false, defaultValue = "0") @Parameter(description = "조회할 주차 (0: 이번주, 1: 지난주, 2: 지지난주, ... 최대 7)") int weeksAgo)
    {

        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }

        Long userId = customUserDetails.getUserId();

        // weeksAgo 범위 검증 (0~7, 음수나 8 이상은 이번 주로 처리)
        int validWeeksAgo = Math.max(0, Math.min(weeksAgo, 7));

        // weeksAgo에 따라 해당 주의 월요일~일요일 계산
        LocalDate today = LocalDate.now();
        LocalDate targetMonday = today.minusWeeks(validWeeksAgo).with(DayOfWeek.MONDAY);
        LocalDate targetSunday = targetMonday.plusDays(6);

        return ResponseEntity.ok(
                DataResponse.from(myPageService.getLearningReport(userId, targetMonday, targetSunday, validWeeksAgo)));
    }

    @Operation(summary = "알림 설정 조회", description = "알림 ON/OFF 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
    })
    @GetMapping("/me/notification")
    public ResponseEntity<DataResponse<NotificationResponse>> getNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }
        return ResponseEntity.ok(
                DataResponse.from(myPageService.getNotificationSetting(customUserDetails.getUserId())));
    }

    @Operation(summary = "알림 설정 변경", description = "알림 ON/OFF 상태를 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 변경 성공")
    })
    @PutMapping("/me/notification")
    public ResponseEntity<DataResponse<Void>> updateNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateNotificationRequest request
    ) {
        if (customUserDetails == null) {
            throw new MypageException(MypageErrorCode.UNAUTHORIZED_ACCESS);
        }
        myPageService.updateNotificationSetting(customUserDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.ok());
    }
}
