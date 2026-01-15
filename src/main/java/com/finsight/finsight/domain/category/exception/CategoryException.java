package com.finsight.finsight.domain.category.exception;

import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.BaseErrorCode;

public class CategoryException extends AppException {
    public CategoryException(BaseErrorCode code) {
        super(code);
    }
}
