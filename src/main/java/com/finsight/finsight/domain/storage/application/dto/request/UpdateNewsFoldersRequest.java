package com.finsight.finsight.domain.storage.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateNewsFoldersRequest(
        @NotEmpty(message = "폴더를 1개 이상 선택해주세요.")
        List<Long> folderIds
) {}
