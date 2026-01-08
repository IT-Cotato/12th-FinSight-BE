package com.finsight.finsight.domain.ai.domain.service;

import com.finsight.finsight.domain.ai.application.dto.response.*;
import com.finsight.finsight.domain.ai.exception.code.AiErrorCode;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import com.finsight.finsight.domain.ai.persistence.repository.*;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleAiQueryService {

    private final NaverArticleRepository naverArticleRepository;
    private final AiArticleSummaryRepository aiArticleSummaryRepository;
    private final AiTermCardRepository aiTermCardRepository;
    private final AiArticleInsightRepository aiArticleInsightRepository;
    private final AiQuizSetRepository aiQuizSetRepository;

    public ArticleAiResultResponse getAiResultRequireAll(Long articleId) {
        NaverArticleEntity article = naverArticleRepository.findById(articleId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        var summaryOpt = aiArticleSummaryRepository.findTopByArticleIdOrderByCreatedAtDesc(articleId);
        var insightOpt = aiArticleInsightRepository.findTopByArticleIdOrderByCreatedAtDesc(articleId);
        var quizContentOpt = aiQuizSetRepository.findTopByArticleIdAndQuizKindOrderByCreatedAtDesc(articleId, AiJobType.QUIZ_CONTENT);
        var quizTermOpt = aiQuizSetRepository.findTopByArticleIdAndQuizKindOrderByCreatedAtDesc(articleId, AiJobType.QUIZ_TERM);

        List<com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity> cards =
                aiTermCardRepository.findByArticleIdOrderByCardOrderAsc(articleId);

        boolean ready = summaryOpt.isPresent()
                && insightOpt.isPresent()
                && quizContentOpt.isPresent()
                && quizTermOpt.isPresent()
                && cards.size() >= 3;

        if (!ready) {
            throw new AppException(AiErrorCode.AI_RESULT_NOT_READY);
        }

        var summary = summaryOpt.get();
        var insight = insightOpt.get();
        var quizContent = quizContentOpt.get();
        var quizTerm = quizTermOpt.get();

        return ArticleAiResultResponse.of(
                article,
                summary.getSummary3Lines(),
                summary.getSummaryFull(),
                cards.stream().map(TermCardResponse::from).toList(),
                insight.getInsightJson(),
                List.of(
                        QuizResponse.from(quizContent),
                        QuizResponse.from(quizTerm)
                )
        );
    }
}
