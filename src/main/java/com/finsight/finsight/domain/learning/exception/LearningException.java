package com.finsight.finsight.domain.learning.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class LearningException extends AppException {
    public LearningException(BaseErrorCode code) {
        super(code);
    }
}
