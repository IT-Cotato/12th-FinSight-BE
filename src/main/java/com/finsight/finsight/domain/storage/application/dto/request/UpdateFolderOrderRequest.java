package com.finsight.finsight.domain.storage.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateFolderOrderRequest(
        @NotBlank(message = "폴더 타입을 선택해주세요.")
        String folderType,

        @NotEmpty(message = "폴더 순서를 입력해주세요.")
        List<FolderOrder> folders
) {
    public record FolderOrder(
            Long folderId,
            Integer sortOrder
    ) {}
}
