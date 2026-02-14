package com.finsight.finsight.domain.notification.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class NotificationException extends AppException {

    public NotificationException(BaseErrorCode code) {
        super(code);
    }
}
