package com.finsight.finsight.domain.storage.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateFolderRequest(
        @NotBlank(message = "폴더 이름을 입력해주세요.")
        String folderName
) {}
