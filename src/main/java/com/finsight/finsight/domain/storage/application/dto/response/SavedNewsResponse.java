package com.finsight.finsight.domain.storage.application.dto.response;

import java.time.LocalDateTime;

public record SavedNewsResponse(
        Long savedItemId,
        Long newsId,
        String category,
        String title,
        String thumbnailUrl,
        LocalDateTime savedAt
) {}
