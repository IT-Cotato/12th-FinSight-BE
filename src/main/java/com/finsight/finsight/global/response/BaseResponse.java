package com.finsight.finsight.global.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

@Getter
public abstract class BaseResponse {

    private final String status;

    protected BaseResponse(HttpStatus status) {
        this.status = status.getReasonPhrase();
    }
}
