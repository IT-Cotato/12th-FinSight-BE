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

    // auth
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다.", "AUTH-011"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.", "AUTH-012"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.", "AUTH-013"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다.", "AUTH-014"),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다.", "AUTH-015"),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "닉네임은 1-10자여야 합니다.", "AUTH-016"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.", "AUTH-017"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 영문, 숫자 조합 6-18자리여야 합니다.", "AUTH-018"),
    INVALID_KAKAO_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 인가 코드입니다.", "AUTH-019"),
    INVALID_KAKAO_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 ID입니다.", "AUTH-020"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", "AUTH-021"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "AUTH-022"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.", "AUTH-023"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "AUTH-024"),

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
