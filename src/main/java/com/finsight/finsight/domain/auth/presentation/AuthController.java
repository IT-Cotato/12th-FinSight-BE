package com.finsight.finsight.domain.auth.presentation;

import com.finsight.finsight.domain.auth.application.dto.request.*;
import com.finsight.finsight.domain.auth.application.dto.response.TokenResponse;
import com.finsight.finsight.domain.auth.domain.service.AuthService;
import com.finsight.finsight.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인")
    public DataResponse<Void> checkNickname(@Valid @RequestBody CheckNicknameRequest request) {
        authService.checkNickname(request.nickname());
        return DataResponse.ok();
    }

    @PostMapping("/send-code")
    @Operation(summary = "인증번호 발송")
    public DataResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request.email());
        return DataResponse.ok();
    }

    @PostMapping("/verify-code")
    @Operation(summary = "인증번호 확인")
    public DataResponse<Void> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.email(), request.code());
        return DataResponse.ok();
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public DataResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return DataResponse.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public DataResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return DataResponse.from(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급")
    public DataResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return DataResponse.from(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public DataResponse<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return DataResponse.ok();
    }

    @PostMapping("/kakao/login")
    @Operation(summary = "카카오 로그인")
    public DataResponse<?> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        TokenResponse response = authService.kakaoLogin(request.code());
        if (response == null) {
            return DataResponse.ok();
        }
        return DataResponse.from(response);
    }

    @PostMapping("/kakao/signup")
    @Operation(summary = "카카오 회원가입")
    public DataResponse<TokenResponse> kakaoSignup(@Valid @RequestBody KakaoSignupRequest request) {
        TokenResponse response = authService.kakaoSignup(request.code(), request.nickname());
        return DataResponse.from(response);
    }
}