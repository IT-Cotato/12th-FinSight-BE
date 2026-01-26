package com.finsight.finsight.domain.quiz.exception;

import com.finsight.finsight.domain.quiz.exception.code.QuizErrorCode;
import com.finsight.finsight.global.exception.AppException;

public class QuizException extends AppException {
    
    public QuizException(QuizErrorCode errorCode) {
        super(errorCode);
    }
}
