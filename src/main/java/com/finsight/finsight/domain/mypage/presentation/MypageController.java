package com.finsight.finsight.domain.mypage.presentation;

import com.finsight.finsight.domain.mypage.application.dto.request.UpdateProfileRequest;
import com.finsight.finsight.domain.mypage.application.dto.response.MypageResponse;
import com.finsight.finsight.domain.mypage.domain.service.MypageService;
import com.finsight.finsight.domain.mypage.exception.MypageException;
import com.finsight.finsight.domain.mypage.exception.code.MypageErrorCode;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody com.finsight.finsight.domain.auth.application.dto.request.CheckNicknameRequest request
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
}
