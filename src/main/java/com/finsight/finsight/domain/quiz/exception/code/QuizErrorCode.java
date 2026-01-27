package com.finsight.finsight.domain.quiz.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QuizErrorCode implements BaseErrorCode {

    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다.", "QUIZ-001"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "QUIZ-002"),
    INVALID_QUIZ_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 퀴즈 타입입니다.", "QUIZ-003"),
    INVALID_ANSWER(HttpStatus.BAD_REQUEST, "유효하지 않은 답안입니다.", "QUIZ-004");

    private final HttpStatus httpStatus;
    private final String message;
    private final String code;
}
