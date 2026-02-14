package com.finsight.finsight.domain.notification.exception;

import com.finsight.finsight.domain.notification.exception.code.NotificationErrorCode;
import com.finsight.finsight.global.exception.AppException;

public class NotificationException extends AppException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
