package com.finsight.finsight.domain.quiz.application.dto.response;

import java.util.List;

/**
 * 퀴즈 조회 응답
 */
public record QuizResponse(
    Long naverArticleId,
    String quizType,
    List<QuestionItem> questions
) {
    /** 개별 문제 */
    public record QuestionItem(
        int questionIndex,        // 문제 번호
        String question,          // 문제 텍스트
        List<String> options,     // 선택지 4개
        Boolean previousCorrect   // 직전 풀이 정답 여부 (null=처음)
    ) {}
}
