package com.finsight.finsight.domain.quiz.application.dto.response;

import java.util.List;

/**
 * 퀴즈 제출 응답 (채점 결과)
 */
public record QuizSubmitResponse(
    int correctCount,   // 맞춘 개수
    int setScore,       // 이번 세트 점수 (최대 50)
    int totalExp,       // 현재 레벨 경험치
    int level,          // 현재 레벨
    List<QuestionResult> results
) {
    /** 개별 문제 결과 */
    public record QuestionResult(
        int questionIndex,         // 문제 번호
        boolean correct,           // 정답 여부
        int selectedIndex,         // 선택한 보기
        int answerIndex,           // 정답 보기
        String question,           // 문제 텍스트
        List<String> options,      // 선택지 4개
        List<String> explanations  // 선택지별 해설 4개
    ) {}
}
