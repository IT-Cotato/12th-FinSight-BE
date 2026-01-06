package com.finsight.finsight.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //400
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", "COMMON-001"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 잘못되었습니다.", "COMMON-002"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없습니다.", "COMMON-003"),

    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부에서 에러가 발생하였습니다.", "COMMON-004"),

    // 502/503 성격(외부 의존 실패)
    NAVER_LIST_FETCH_FAIL(HttpStatus.BAD_GATEWAY, "네이버 목록 페이지 조회 실패", "NAVER-001"),
    NAVER_ARTICLE_FETCH_FAIL(HttpStatus.BAD_GATEWAY, "네이버 기사 페이지 조회 실패", "NAVER-002"),
    NAVER_ARTICLE_PARSE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 기사 파싱 실패", "NAVER-003"),

    // AI 잡
    AI_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 작업을 찾을 수 없습니다.", "AI-001"),
    OPENAI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "OpenAI 호출 제한에 걸렸습니다.", "AI-002"),
    OPENAI_API_FAIL(HttpStatus.BAD_GATEWAY, "OpenAI API 호출에 실패했습니다.", "AI-003"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
