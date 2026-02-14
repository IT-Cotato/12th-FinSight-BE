package com.finsight.finsight.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank(message = "인증 코드는 필수입니다.")
        String code
) {}