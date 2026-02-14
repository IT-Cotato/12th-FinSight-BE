package com.finsight.finsight.domain.storage.application.dto.response;

import java.time.LocalDateTime;

public record SavedTermResponse(
        Long savedItemId,
        Long termId,
        String term,
        String description,
        LocalDateTime savedAt
) {}
