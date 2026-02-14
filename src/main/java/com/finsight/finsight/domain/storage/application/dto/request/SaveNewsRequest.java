package com.finsight.finsight.domain.storage.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveNewsRequest(
        @NotNull(message = "뉴스를 선택해주세요.")
        Long articleId,

        @NotEmpty(message = "폴더를 1개 이상 선택해주세요.")
        List<Long> folderIds
) {}
