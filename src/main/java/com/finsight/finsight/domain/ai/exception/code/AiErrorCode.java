package com.finsight.finsight.domain.ai.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements BaseErrorCode {

    // AI 잡
    AI_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "AI-001", "AI 작업을 찾을 수 없습니다."),
    AI_RESULT_NOT_READY(HttpStatus.CONFLICT, "AI-004", "AI 결과 생성이 완료되지 않았습니다."),
    AI_JOB_NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "AI-005", "SUSPENDED 상태가 아닌 작업은 재개할 수 없습니다."),

    // Admin
    ADMIN_TOKEN_INVALID(HttpStatus.FORBIDDEN, "AI-401", "관리자 토큰이 유효하지 않습니다."),

    // OpenAI - 재시도 가능 (RETRY_WAIT)
    OPENAI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "AI-002", "OpenAI 호출 제한에 걸렸습니다."),
    OPENAI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AI-010", "OpenAI 서버 오류가 발생했습니다."),
    OPENAI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI-011", "OpenAI 응답 시간이 초과되었습니다."),

    // OpenAI - 재시도 불가 (SUSPENDED) - 쿼터/결제/인증
    OPENAI_QUOTA_EXHAUSTED(HttpStatus.PAYMENT_REQUIRED, "AI-020", "OpenAI API 크레딧이 소진되었습니다."),
    OPENAI_INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "AI-021", "OpenAI 계정 잔액이 부족합니다."),
    OPENAI_INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "AI-022", "OpenAI API 키가 유효하지 않습니다."),
    OPENAI_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AI-023", "OpenAI API 접근이 거부되었습니다."),

    // OpenAI - 재시도 불가 (FAILED) - 잘못된 요청/파싱, Admin
    OPENAI_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "AI-024", "OpenAI 요청 형식이 잘못되었습니다."),
    OPENAI_RESPONSE_PARSE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "AI-030", "OpenAI 응답 파싱에 실패했습니다."),

    // 일반 실패 (fallback)
    OPENAI_API_FAIL(HttpStatus.BAD_GATEWAY, "AI-003", "OpenAI API 호출에 실패했습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    /**
     * 재시도 가능한 에러인지 확인 (429, 5xx, timeout)
     */
    public boolean isRetryable() {
        return switch (this) {
            case OPENAI_RATE_LIMIT,
                 OPENAI_SERVER_ERROR,
                 OPENAI_TIMEOUT -> true;
            default -> false;
        };
    }

    /**
     * 재시도 불가 + SUSPENDED 전환 대상 (쿼터/결제/인증)
     */
    public boolean isSuspendable() {
        return switch (this) {
            case OPENAI_QUOTA_EXHAUSTED,
                 OPENAI_INSUFFICIENT_BALANCE,
                 OPENAI_INVALID_API_KEY,
                 OPENAI_ACCESS_DENIED -> true;
            default -> false;
        };
    }

    /**
     * 쿼터/결제 관련 에러인지 확인
     */
    public boolean isQuotaRelated() {
        return this == OPENAI_QUOTA_EXHAUSTED || this == OPENAI_INSUFFICIENT_BALANCE;
    }
}