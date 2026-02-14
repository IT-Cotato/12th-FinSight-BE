package com.finsight.finsight.domain.auth.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class AuthException extends AppException {
    public AuthException(BaseErrorCode code) {
        super(code);
    }
}
