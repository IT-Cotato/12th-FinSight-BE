package com.finsight.finsight.domain.example.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class ExampleException extends AppException {
    public ExampleException(BaseErrorCode code) {
        super(code);
    }
}
