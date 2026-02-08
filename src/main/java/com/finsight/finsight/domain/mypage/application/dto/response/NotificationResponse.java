package com.finsight.finsight.domain.mypage.application.dto.response;

import lombok.Builder;

@Builder
public record NotificationResponse(
        Boolean enabled
) {}
