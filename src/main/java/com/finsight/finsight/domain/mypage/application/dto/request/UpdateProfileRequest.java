package com.finsight.finsight.domain.mypage.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateProfileRequest(
        @Schema(description = "변경할 닉네임", example = "newNick") @NotBlank(message = "닉네임은 필수입니다.") String nickname,

        @Schema(description = "관심 카테고리 목록", example = "[\"STOCK\", \"GLOBAL\", \"INDUSTRY\"]") @NotNull(message = "관심 카테고리는 필수입니다.") List<String> categories) {
}
