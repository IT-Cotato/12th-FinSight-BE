package com.finsight.finsight.domain.mypage.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationRequest(
        @NotNull(message = "알림 설정 값은 필수입니다.")
        Boolean enabled
) {}
