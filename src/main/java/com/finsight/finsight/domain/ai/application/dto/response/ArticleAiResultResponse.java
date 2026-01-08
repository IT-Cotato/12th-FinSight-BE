package com.finsight.finsight.domain.ai.application.dto.response;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleAiResultResponse(
        Long articleId,
        String category,
        String title,
        String press,
        LocalDateTime publishedAt,
        String thumbnailUrl,

        String summary3Lines,
        String summaryFull,

        List<TermCardResponse> termCards,
        String insightJson,
        List<QuizResponse> quizzes
) {
    public static ArticleAiResultResponse of(
            NaverArticleEntity article,
            String summary3Lines,
            String summaryFull,
            List<TermCardResponse> termCards,
            String insightJson,
            List<QuizResponse> quizzes
    ) {
        return new ArticleAiResultResponse(
                article.getId(),
                article.getCategory(),
                article.getTitle(),
                article.getPress(),
                article.getPublishedAt(),
                article.getThumbnailUrl(),
                summary3Lines,
                summaryFull,
                termCards,
                insightJson,
                quizzes
        );
    }
}