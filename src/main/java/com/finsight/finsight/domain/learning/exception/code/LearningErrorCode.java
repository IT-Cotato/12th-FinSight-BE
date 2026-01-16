package com.finsight.finsight.domain.learning.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LearningErrorCode implements BaseErrorCode {

    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "LEARNING-001", "잘못된 커서 형식입니다."),
    CURSOR_ENCODING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LEARNING-002", "커서 인코딩 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
