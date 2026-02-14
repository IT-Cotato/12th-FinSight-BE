package com.finsight.finsight.domain.term.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TermErrorCode implements BaseErrorCode {
    TREM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM-001", "id에 해당하는 용어를 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
