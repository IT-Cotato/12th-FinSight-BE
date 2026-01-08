package com.finsight.finsight.global.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public AppException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
