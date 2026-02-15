package com.finsight.finsight.domain.notification.application.dto.request;

import com.finsight.finsight.domain.notification.domain.constant.DeviceType;
import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String fcmToken,

        DeviceType deviceType  // WEB, ANDROID, IOS (선택)
) {}