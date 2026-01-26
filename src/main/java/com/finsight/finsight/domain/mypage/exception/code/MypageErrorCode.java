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
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-003", "이미 사용 중인 닉네임입니다.");

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
