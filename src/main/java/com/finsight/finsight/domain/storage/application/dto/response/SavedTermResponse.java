package com.finsight.finsight.domain.storage.application.dto.response;

import java.time.LocalDateTime;

public record SavedTermResponse(
        Long savedItemId,
        Long termId,
        String displayName,
        String definition,
        LocalDateTime savedAt
) {}
