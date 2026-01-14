package com.finsight.finsight.domain.auth.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다.", "AUTH-001"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.", "AUTH-002"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.", "AUTH-003"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다.", "AUTH-004"),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다.", "AUTH-005"),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "닉네임은 1-10자여야 합니다.", "AUTH-006"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.", "AUTH-007"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 영문, 숫자 조합 6-18자리여야 합니다.", "AUTH-008"),
    INVALID_KAKAO_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 인가 코드입니다.", "AUTH-009"),
    INVALID_KAKAO_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 ID입니다.", "AUTH-010"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", "AUTH-011"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "AUTH-012"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.", "AUTH-013"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "AUTH-014"),
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "닉네임은 필수 입력값입니다.", "AUTH-015"),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일은 필수 입력값입니다.", "AUTH-016"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비밀번호는 필수 입력값입니다.", "AUTH-017"),
    VERIFICATION_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "인증번호는 필수 입력값입니다.", "AUTH-018"),
    KAKAO_ID_REQUIRED(HttpStatus.BAD_REQUEST, "카카오 ID는 필수 입력값입니다.", "AUTH-019")
    ;

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
