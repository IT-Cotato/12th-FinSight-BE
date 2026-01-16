package com.finsight.finsight.domain.naver.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class NaverArticleException extends AppException {
    public NaverArticleException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
