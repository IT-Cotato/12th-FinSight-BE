package com.finsight.finsight.domain.notification.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-001", "FCM 토큰을 찾을 수 없습니다."),
    FCM_TOKEN_NOT_OWNED(HttpStatus.FORBIDDEN, "NOTIFICATION-002", "본인의 토큰만 삭제할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
