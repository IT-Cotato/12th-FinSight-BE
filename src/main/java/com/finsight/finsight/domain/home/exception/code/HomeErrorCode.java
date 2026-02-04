package com.finsight.finsight.domain.home.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HomeErrorCode implements BaseErrorCode {

    CATEGORY_NOT_IN_USER_INTEREST(HttpStatus.BAD_REQUEST, "사용자의 관심 카테고리에 포함되지 않은 카테고리입니다.", "HOME-001");

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
