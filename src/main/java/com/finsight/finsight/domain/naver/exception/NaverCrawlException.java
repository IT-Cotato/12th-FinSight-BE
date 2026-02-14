package com.finsight.finsight.domain.naver.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class NaverCrawlException extends AppException {
    public NaverCrawlException(BaseErrorCode errorCode){ super(errorCode); }
}
