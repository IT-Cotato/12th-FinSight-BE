package com.finsight.finsight.domain.mypage.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MypageErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "유저 정보를 찾지 못했습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "USER-002", "허용되지 않은 유저입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-003", "이미 사용 중인 닉네임입니다."),

    // 비밀번호 변경
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "USER-004", "현재 비밀번호가 일치하지 않습니다."),
    INVALID_NEW_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "USER-005", "비밀번호는 영문, 숫자 조합 6-18자리여야 합니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "USER-006", "현재 비밀번호와 동일합니다."),
    KAKAO_PASSWORD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER-007", "카카오 계정은 비밀번호를 변경할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
