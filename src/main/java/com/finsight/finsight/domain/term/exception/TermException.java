package com.finsight.finsight.domain.term.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class TermException extends AppException {
    public TermException(BaseErrorCode code) {
        super(code);
    }
}
