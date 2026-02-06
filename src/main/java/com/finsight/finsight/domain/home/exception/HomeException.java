package com.finsight.finsight.domain.home.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class HomeException extends AppException {
    public HomeException(BaseErrorCode code) {
        super(code);
    }
}
