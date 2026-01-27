package com.finsight.finsight.domain.storage.exception;

import com.finsight.finsight.domain.storage.exception.code.StorageErrorCode;
import com.finsight.finsight.global.exception.AppException;

public class StorageException extends AppException {

    public StorageException(StorageErrorCode errorCode) {
        super(errorCode);
    }
}
