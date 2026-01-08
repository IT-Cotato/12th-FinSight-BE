package com.finsight.finsight.domain.ai.application.dto.response;

import com.finsight.finsight.domain.ai.persistence.entity.AiQuizSetEntity;

public record QuizResponse(
        String kind,
        String quizJson
) {
    public static QuizResponse from(AiQuizSetEntity e) {
        return new QuizResponse(
                e.getQuizKind().name(),
                e.getQuizJson()
        );
    }
}
