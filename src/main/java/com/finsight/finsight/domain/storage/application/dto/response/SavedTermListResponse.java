package com.finsight.finsight.domain.storage.application.dto.response;

import java.util.List;

public record SavedTermListResponse(
        List<SavedTermResponse> terms,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext
) {}
