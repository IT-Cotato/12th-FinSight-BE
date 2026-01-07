package com.finsight.finsight.global.exception;


import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseErrorCode{

    //400
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", "COMMON-001"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 잘못되었습니다.", "COMMON-002"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", "COMMON-003"),

    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 에러가 발생하였습니다.", "COMMON-004"),



    // AI 잡
    AI_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 작업을 찾을 수 없습니다.", "AI-001"),
    OPENAI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "OpenAI 호출 제한에 걸렸습니다.", "AI-002"),
    OPENAI_API_FAIL(HttpStatus.BAD_GATEWAY, "OpenAI API 호출에 실패했습니다.", "AI-003"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
