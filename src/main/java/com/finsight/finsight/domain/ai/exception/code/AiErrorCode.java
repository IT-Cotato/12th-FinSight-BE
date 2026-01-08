package com.finsight.finsight.domain.ai.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements BaseErrorCode {

    // AI 잡
    AI_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 작업을 찾을 수 없습니다.", "AI-001"),
    OPENAI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "OpenAI 호출 제한에 걸렸습니다.", "AI-002"),
    OPENAI_API_FAIL(HttpStatus.BAD_GATEWAY, "OpenAI API 호출에 실패했습니다.", "AI-003"),
    AI_RESULT_NOT_READY(HttpStatus.CONFLICT, "AI 결과 생성이 완료되지 않았습니다.", "AI-004"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}