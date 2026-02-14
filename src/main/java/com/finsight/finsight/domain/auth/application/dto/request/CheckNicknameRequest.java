package com.finsight.finsight.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckNicknameRequest(
        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        String nickname
) {}