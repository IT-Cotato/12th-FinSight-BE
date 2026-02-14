package com.finsight.finsight.domain.storage.application.dto.response;

import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;

public record FolderResponse(
        Long folderId,
        String folderType,
        String folderName,
        Integer sortOrder,
        Long itemCount
) {
    // 폴더 생성용 (itemCount 없이)
    public static FolderResponse from(FolderEntity entity) {
        return new FolderResponse(
                entity.getFolderId(),
                entity.getFolderType().name(),
                entity.getFolderName(),
                entity.getSortOrder(),
                null
        );
    }

    // 폴더 조회/수정용 (itemCount 포함)
    public static FolderResponse from(FolderEntity entity, Long itemCount) {
        return new FolderResponse(
                entity.getFolderId(),
                entity.getFolderType().name(),
                entity.getFolderName(),
                entity.getSortOrder(),
                itemCount
        );
    }
}
