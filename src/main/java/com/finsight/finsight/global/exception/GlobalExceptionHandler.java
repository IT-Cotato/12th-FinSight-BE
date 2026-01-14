package com.finsight.finsight.global.exception;

import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.global.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * Bean Validation 단계에서는 "필수 입력값 누락"만 처리한다.
     * 필드명 -> REQUIRED ErrorCode 매핑 테이블
     */
    private static final Map<String, BaseErrorCode> REQUIRED_FIELD_ERROR_MAP =
            Map.of(
                    "nickname", AuthErrorCode.NICKNAME_REQUIRED,
                    "email", AuthErrorCode.EMAIL_REQUIRED,
                    "password", AuthErrorCode.PASSWORD_REQUIRED,
                    "code", AuthErrorCode.VERIFICATION_CODE_REQUIRED,
                    "kakaoId", AuthErrorCode.KAKAO_ID_REQUIRED
            );

    // 처리되지 않은 모든 예외를 잡는 핸들러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외 발생: ", e);
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppCustomException(AppException e, HttpServletRequest request) {
        log.error("AppException 발생: {}", e.getErrorCode().getMessage());
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode(), request);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * Bean Validation 예외
     * - 필수값 누락만 처리
     * - 형식/정책 검증은 Service 레이어 책임
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.error("Validation 예외 발생");
        log.error("요청 정보: {} {}", request.getMethod(), request.getRequestURI());

        // 첫 번째 Validation 에러의 필드명
        String field = e.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getField();

        // 필드명 기반 REQUIRED ErrorCode 조회
        BaseErrorCode errorCode =
                REQUIRED_FIELD_ERROR_MAP.getOrDefault(
                        field,
                        ErrorCode.INVALID_PARAMETER
                );

        ErrorResponse errorResponse =
                ErrorResponse.of(errorCode, request);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }


}
