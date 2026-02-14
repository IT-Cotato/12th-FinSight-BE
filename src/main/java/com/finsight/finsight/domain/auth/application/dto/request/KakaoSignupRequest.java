package com.finsight.finsight.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KakaoSignupRequest(
        @NotBlank(message = "카카오 ID는 필수입니다.")
        String kakaoId,

        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {}