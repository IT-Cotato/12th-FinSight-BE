package com.finsight.finsight.domain.category.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CategoryErrorCode implements BaseErrorCode {

    MINIMUM_CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "3개 이상 선택해주세요.", "CATEGORY-001"),
    INVALID_CATEGORY_SECTION(HttpStatus.BAD_REQUEST, "유효하지 않은 관심분야입니다.", "CATEGORY-002");

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
