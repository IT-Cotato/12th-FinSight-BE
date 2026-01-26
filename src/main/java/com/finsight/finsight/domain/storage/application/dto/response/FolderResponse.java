package com.finsight.finsight.domain.storage.application.dto.response;

import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;

public record FolderResponse(
        Long folderId,
        String folderType,
        String folderName,
        Integer sortOrder
) {
    public static FolderResponse from(FolderEntity entity) {
        return new FolderResponse(
                entity.getFolderId(),
                entity.getFolderType().name(),
                entity.getFolderName(),
                entity.getSortOrder()
        );
    }
}
