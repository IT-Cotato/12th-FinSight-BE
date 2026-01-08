package com.finsight.finsight.domain.ai.application.dto.response;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobStatus;

public record ArticleAiStatusResponse(
        Long articleId,
        boolean allSuccess,
        AiJobStatus summaryStatus,
        AiJobStatus termCardsStatus,
        AiJobStatus insightStatus,
        AiJobStatus quizContentStatus,
        AiJobStatus quizTermStatus
) {}
