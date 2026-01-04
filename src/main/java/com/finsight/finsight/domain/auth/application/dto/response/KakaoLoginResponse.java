package com.finsight.finsight.domain.auth.application.dto.response;

public record KakaoLoginResponse(
        String accessToken,
        String refreshToken,
        String kakaoId,
        boolean isNewUser
) {
    // 기존 회원 -> 바로 로그인하기 위해 token 반환
    public static KakaoLoginResponse of(String accessToken, String refreshToken) {
        return new KakaoLoginResponse(accessToken, refreshToken, null, false);
    }

    // 신규 회원 -> 회원가입 진행을 위해 kakaoId 반환
    public static KakaoLoginResponse newUser(String kakaoId) {
        return new KakaoLoginResponse(null, null, kakaoId, true);
    }
}