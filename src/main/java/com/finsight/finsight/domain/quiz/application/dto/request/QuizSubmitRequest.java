package com.finsight.finsight.domain.quiz.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 퀴즈 제출 요청
 */
public record QuizSubmitRequest(
    
    @NotNull(message = "뉴스 ID는 필수입니다.")
    Long naverArticleId,
    
    @NotNull(message = "퀴즈 타입은 필수입니다.")
    String quizType,  // CONTENT 또는 TERM
    
    @NotNull(message = "답안은 필수입니다.")
    List<AnswerItem> answers
) {
    /** 개별 답안 */
    public record AnswerItem(
        int questionIndex,   // 문제 번호 (0, 1, 2)
        int selectedIndex    // 선택한 보기 (0~3)
    ) {}
}
