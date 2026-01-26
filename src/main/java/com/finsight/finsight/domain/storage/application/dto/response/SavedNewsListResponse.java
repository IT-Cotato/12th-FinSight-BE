package com.finsight.finsight.domain.storage.application.dto.response;

import java.util.List;

public record SavedNewsListResponse(
        List<SavedNewsResponse> news,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext
) {}
