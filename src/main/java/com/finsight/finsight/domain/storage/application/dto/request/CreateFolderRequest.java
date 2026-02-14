package com.finsight.finsight.domain.storage.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateFolderRequest(
        @NotBlank(message = "폴더 타입을 선택해주세요.")
        String folderType,

        @NotBlank(message = "폴더 이름을 입력해주세요.")
        String folderName
) {}
