package com.finsight.finsight.domain.mypage.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class MypageException extends AppException {
    public MypageException(BaseErrorCode code) {
        super(code);
    }
}
