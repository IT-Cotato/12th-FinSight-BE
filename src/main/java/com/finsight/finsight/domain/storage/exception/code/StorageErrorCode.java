package com.finsight.finsight.domain.storage.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StorageErrorCode implements BaseErrorCode {

    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE-001", "폴더를 찾을 수 없습니다."),
    FOLDER_NAME_DUPLICATE(HttpStatus.CONFLICT, "STORAGE-002", "이미 존재하는 폴더 이름입니다."),
    FOLDER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "STORAGE-003", "폴더는 최대 10개까지 생성할 수 있습니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE-004", "저장된 항목을 찾을 수 없습니다."),
    ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "STORAGE-005", "이미 저장된 항목입니다."),
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE-006", "뉴스를 찾을 수 없습니다."),
    ALREADY_SAVED(HttpStatus.CONFLICT, "STORAGE-007", "이미 저장된 뉴스입니다."),
    SAVED_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE-008", "저장된 항목을 찾을 수 없습니다."),
    FOLDER_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "STORAGE-009", "폴더 타입이 일치하지 않습니다."),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE-010", "용어를 찾을 수 없습니다."),
    ALREADY_SAVED_TERM(HttpStatus.CONFLICT, "STORAGE-011", "이미 저장된 용어입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
