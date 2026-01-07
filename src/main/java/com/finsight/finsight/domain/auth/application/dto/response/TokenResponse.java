package com.finsight.finsight.domain.auth.application.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}