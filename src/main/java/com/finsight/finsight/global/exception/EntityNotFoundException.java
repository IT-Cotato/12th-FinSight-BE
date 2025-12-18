package com.finsight.finsight.global.exception;

public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
