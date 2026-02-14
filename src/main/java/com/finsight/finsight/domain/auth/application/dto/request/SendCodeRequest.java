package com.finsight.finsight.domain.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendCodeRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String email
) {}