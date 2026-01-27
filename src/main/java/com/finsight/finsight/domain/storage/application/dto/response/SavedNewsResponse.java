package com.finsight.finsight.domain.storage.application.dto.response;

import java.time.LocalDateTime;

public record SavedNewsResponse(
        Long savedItemId,
        Long articleId,
        String title,
        String press,
        String section,
        String thumbnailUrl,
        LocalDateTime publishedAt,
        LocalDateTime savedAt
) {}
