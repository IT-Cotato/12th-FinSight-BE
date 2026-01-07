package com.finsight.finsight.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckNicknameRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 10, message = "닉네임은 1-10자여야 합니다.")
        String nickname
) {}