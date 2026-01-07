package com.finsight.finsight.domain.ai.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.finsight.domain.ai.domain.client.OpenAiClient;
import com.finsight.finsight.domain.ai.domain.metrics.AiMetrics;
import com.finsight.finsight.domain.ai.domain.prompt.AiPrompts;
import com.finsight.finsight.domain.ai.domain.prompt.AiSchemas;
import com.finsight.finsight.domain.ai.persistence.entity.*;
import com.finsight.finsight.domain.ai.persistence.repository.*;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.term.domain.service.TermService;
import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJobService {

    private static final ObjectMapper OM = new ObjectMapper();

    private final AiJobRepository aiJobRepository;
    private final AiArticleSummaryRepository aiArticleSummaryRepository;

    private final AiTermCardRepository aiTermCardRepository;
    private final AiArticleInsightRepository aiArticleInsightRepository;
    private final AiQuizSetRepository aiQuizSetRepository;

    private final TermService termService;

    private final OpenAiClient openAiClient;
    private final AiMetrics metrics;
    private final MeterRegistry meterRegistry;

    // =========================
    // 1) Worker가 호출하는 claim 메서드
    // =========================
    @Transactional
    public List<Long> claimNextJobIds(AiJobType type, int limit) {
        List<Long> ids = aiJobRepository.findPendingIdsForUpdateSkipLocked(type.name(), limit);
        if (ids.isEmpty()) return List.of();

        int updated = aiJobRepository.markRunning(ids);

        Counter.builder("ai_jobs_claim_total")
                .tag("type", type.name())
                .register(meterRegistry)
                .increment(ids.size());

        log.info("[AI] claim type={} requested={} updated={}", type, ids.size(), updated);
        return ids;
    }

    // =========================
    // 2) ENQUEUE (크롤러는 SUMMARY만 enqueue)
    // =========================
    @Transactional
    public void enqueueSummary(NaverArticleEntity article, String promptVersion, String model) {
        enqueue(article, AiJobType.SUMMARY, promptVersion, model);
    }

    @Transactional
    public void enqueueDownstreamAfterSummary(NaverArticleEntity article, String promptVersion, String model) {
        enqueue(article, AiJobType.TERM_CARDS, promptVersion, model);
        enqueue(article, AiJobType.INSIGHT, promptVersion, model);
        enqueue(article, AiJobType.QUIZ_CONTENT, promptVersion, model);
    }

    @Transactional
    public void enqueueQuizTermAfterTermCards(NaverArticleEntity article, String promptVersion, String model) {
        enqueue(article, AiJobType.QUIZ_TERM, promptVersion, model);
    }

    @Transactional
    public void enqueue(NaverArticleEntity article, AiJobType type, String promptVersion, String model) {
        try {
            aiJobRepository.save(AiJobEntity.pending(article, type, promptVersion, model));
            metrics.incEnqueue(type, "ok");
        } catch (DataIntegrityViolationException dup) {
            // uk_ai_job_article_type_version 레이스/중복 방어
            metrics.incEnqueue(type, "dup");
        }
    }

    // =========================
    // SUMMARY
    // =========================
    public void processSummary(Long jobId) {
        Timer.Sample sample = metrics.startTimer();
        try {
            AiJobEntity job = loadRunningJob(jobId, AiJobType.SUMMARY);
            NaverArticleEntity article = job.getArticle();

            // 본문 없으면 실패
            if (isBlank(article.getContent())) throw new AppException(ErrorCode.NAVER_ARTICLE_PARSE_FAIL);

            JsonNode response = openAiClient.createJsonSchemaResponse(
                    List.of(
                            Map.of("role", "system", "content", AiPrompts.COMMON_SYSTEM),
                            Map.of("role", "user", "content", AiPrompts.summaryUser(
                                    article.getTitle(),
                                    article.getPress(),
                                    article.getPublishedAt(),
                                    article.getContent()
                            ))
                    ),
                    "article_summary",
                    AiSchemas.summarySchema()
            );

            String jsonText = OpenAiClient.extractOutputText(response);
            if (jsonText == null || jsonText.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            JsonNode parsed = OM.readTree(jsonText);

            JsonNode arr = parsed.path("summary3");
            if (!arr.isArray() || arr.size() != 3) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            List<String> lines = new ArrayList<>(3);
            for (JsonNode n : arr) {
                String line = n.asText(null);
                if (line == null || line.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);
                lines.add(line.trim());
            }

            // 3줄 요약은 배열 → "\n"로 join 해서 문자열로 저장
            String summary3Lines = String.join("\n", lines);

            String summaryFull = parsed.path("summaryFull").asText(null);
            if (summaryFull == null || summaryFull.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            completeSummarySuccess(job, article, summary3Lines, summaryFull);

            metrics.incProcessed(AiJobType.SUMMARY, "success");
        } catch (AppException e) {
            markJobFailed(jobId, e.getErrorCode());
            metrics.incProcessed(AiJobType.SUMMARY, "failed");
        } catch (Exception e) {
            log.error("[AI] summary unexpected error jobId={}", jobId, e);
            markJobFailed(jobId, ErrorCode.OPENAI_API_FAIL);
            metrics.incProcessed(AiJobType.SUMMARY, "failed");
        } finally {
            metrics.stopTimer(sample, AiJobType.SUMMARY);
        }
    }


    @Transactional
    protected void completeSummarySuccess(AiJobEntity job,
                                          NaverArticleEntity article,
                                          String summary3Lines,
                                          String summaryFull) {

        // 이미 저장돼 있으면 성공 처리만
        if (aiArticleSummaryRepository.existsByJobId(job.getId())) {
            job.markSuccess();
            return;
        }

        AiArticleSummaryEntity entity = AiArticleSummaryEntity.builder()
                .job(job)
                .article(article)
                .summary3Lines(summary3Lines)
                .summaryFull(summaryFull)
                .build();

        try {
            aiArticleSummaryRepository.save(entity);
        } catch (DataIntegrityViolationException dup) {
            // uk_ai_summary_job 레이스 방어
        }

        job.markSuccess();

        // ✅ SUMMARY 성공 시 후속 enqueue (TERM/INSIGHT/QUIZ_CONTENT)
        enqueueDownstreamAfterSummary(article, job.getPromptVersion(), job.getModel());
    }

    // =========================
    // TERM_CARDS
    // =========================
    public void processTermCards(Long jobId) {
        Timer.Sample sample = metrics.startTimer();
        try {
            JobAndSummary ctx = loadJobAndSummary(jobId, AiJobType.TERM_CARDS);

            JsonNode response = openAiClient.createJsonSchemaResponse(
                    List.of(
                            Map.of("role", "system", "content", AiPrompts.COMMON_SYSTEM),
                            Map.of("role", "user", "content", AiPrompts.termCardsUser(ctx.summaryFull))
                    ),
                    "term_cards",
                    AiSchemas.termCardsSchema()
            );

            String jsonText = OpenAiClient.extractOutputText(response);
            if (jsonText == null || jsonText.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            JsonNode parsed = OM.readTree(jsonText);
            JsonNode cards = parsed.path("cards");
            if (!cards.isArray() || cards.size() != 3) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            completeTermCardsSuccess(ctx.job, ctx.article, cards);

            metrics.incProcessed(AiJobType.TERM_CARDS, "success");
        } catch (AppException e) {
            markJobFailed(jobId, e.getErrorCode());
            metrics.incProcessed(AiJobType.TERM_CARDS, "failed");
        } catch (Exception e) {
            log.error("[AI] term_cards unexpected error jobId={}", jobId, e);
            markJobFailed(jobId, ErrorCode.OPENAI_API_FAIL);
            metrics.incProcessed(AiJobType.TERM_CARDS, "failed");
        } finally {
            metrics.stopTimer(sample, AiJobType.TERM_CARDS);
        }
    }

    @Transactional
    protected void completeTermCardsSuccess(AiJobEntity job, NaverArticleEntity article, JsonNode cards) {

        if (aiTermCardRepository.existsByJobId(job.getId())) {
            job.markSuccess();
            return;
        }

        int order = 1;
        Set<Long> usedTermIds = new HashSet<>();

        for (JsonNode c : cards) {
            String rawTerm = c.path("term").asText(null);
            String highlightText = c.path("highlightText").asText(null);
            String definition = c.path("definition").asText(null);

            if (isBlank(rawTerm) || isBlank(highlightText) || isBlank(definition)) {
                throw new AppException(ErrorCode.OPENAI_API_FAIL);
            }

            // ✅ 전역 용어 upsert (definition은 전역에만 저장)
            TermEntity termEntity = termService.getOrCreate(rawTerm, definition);

            // 같은 기사 안에서 중복 term 3개 나오는 경우 방어
            if (!usedTermIds.add(termEntity.getId())) continue;

            AiTermCardEntity entity = AiTermCardEntity.builder()
                    .job(job)
                    .article(article)
                    .term(termEntity)
                    .cardOrder(order++)
                    .highlightText(highlightText.trim())
                    .build();

            try {
                aiTermCardRepository.save(entity);
            } catch (DataIntegrityViolationException ignore) {
            }

            if (order > 3) break;
        }

        if (order <= 3) throw new AppException(ErrorCode.OPENAI_API_FAIL);

        job.markSuccess();

        // ✅ TERM_CARDS 성공 시 후속 enqueue (QUIZ_TERM)
        enqueueQuizTermAfterTermCards(article, job.getPromptVersion(), job.getModel());
    }

    // =========================
    // INSIGHT
    // =========================
    public void processInsight(Long jobId) {
        Timer.Sample sample = metrics.startTimer();
        try {
            JobAndSummary ctx = loadJobAndSummary(jobId, AiJobType.INSIGHT);

            JsonNode response = openAiClient.createJsonSchemaResponse(
                    List.of(
                            Map.of("role", "system", "content", AiPrompts.COMMON_SYSTEM),
                            Map.of("role", "user", "content", AiPrompts.insightUser(ctx.summaryFull))
                    ),
                    "article_insight",
                    AiSchemas.insightSchema()
            );

            String jsonText = OpenAiClient.extractOutputText(response);
            if (jsonText == null || jsonText.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            completeInsightSuccess(ctx.job, ctx.article, jsonText);

            metrics.incProcessed(AiJobType.INSIGHT, "success");
        } catch (AppException e) {
            markJobFailed(jobId, e.getErrorCode());
            metrics.incProcessed(AiJobType.INSIGHT, "failed");
        } catch (Exception e) {
            log.error("[AI] insight unexpected error jobId={}", jobId, e);
            markJobFailed(jobId, ErrorCode.OPENAI_API_FAIL);
            metrics.incProcessed(AiJobType.INSIGHT, "failed");
        } finally {
            metrics.stopTimer(sample, AiJobType.INSIGHT);
        }
    }

    @Transactional
    protected void completeInsightSuccess(AiJobEntity job, NaverArticleEntity article, String insightJson) {

        if (aiArticleInsightRepository.existsByJobId(job.getId())) {
            job.markSuccess();
            return;
        }

        AiArticleInsightEntity entity = AiArticleInsightEntity.builder()
                .job(job)
                .article(article)
                .insightJson(insightJson)
                .build();

        try {
            aiArticleInsightRepository.save(entity);
        } catch (DataIntegrityViolationException dup) {
            // uk_ai_insight_job 레이스 방어
        }

        job.markSuccess();
    }

    // =========================
    // QUIZ_CONTENT
    // =========================
    public void processQuizContent(Long jobId) {
        Timer.Sample sample = metrics.startTimer();
        try {
            JobAndSummary ctx = loadJobAndSummary(jobId, AiJobType.QUIZ_CONTENT);

            JsonNode response = openAiClient.createJsonSchemaResponse(
                    List.of(
                            Map.of("role", "system", "content", AiPrompts.COMMON_SYSTEM),
                            Map.of("role", "user", "content", AiPrompts.quizContentUser(ctx.summaryFull))
                    ),
                    "quiz_content",
                    AiSchemas.quizSchema()
            );

            String jsonText = OpenAiClient.extractOutputText(response);
            if (jsonText == null || jsonText.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            completeQuizSuccess(ctx.job, ctx.article, AiJobType.QUIZ_CONTENT, jsonText);

            metrics.incProcessed(AiJobType.QUIZ_CONTENT, "success");
        } catch (AppException e) {
            markJobFailed(jobId, e.getErrorCode());
            metrics.incProcessed(AiJobType.QUIZ_CONTENT, "failed");
        } catch (Exception e) {
            log.error("[AI] quiz_content unexpected error jobId={}", jobId, e);
            markJobFailed(jobId, ErrorCode.OPENAI_API_FAIL);
            metrics.incProcessed(AiJobType.QUIZ_CONTENT, "failed");
        } finally {
            metrics.stopTimer(sample, AiJobType.QUIZ_CONTENT);
        }
    }

    // =========================
    // QUIZ_TERM (TERM_CARDS 기반)
    // =========================
    public void processQuizTerm(Long jobId) {
        Timer.Sample sample = metrics.startTimer();
        try {
            AiJobEntity job = loadRunningJob(jobId, AiJobType.QUIZ_TERM);
            var article = job.getArticle();

            List<AiTermCardEntity> termCards = fetchTermCardsForArticle(article.getId());
            if (termCards.size() < 3) throw new AppException(ErrorCode.NOT_FOUND);

            String termCardsText = buildTermCardsText(termCards);

            JsonNode response = openAiClient.createJsonSchemaResponse(
                    List.of(
                            Map.of("role", "system", "content", AiPrompts.COMMON_SYSTEM),
                            Map.of("role", "user", "content", AiPrompts.quizTermUser(termCardsText))
                    ),
                    "quiz_term",
                    AiSchemas.quizSchema()
            );

            String jsonText = OpenAiClient.extractOutputText(response);
            if (jsonText == null || jsonText.isBlank()) throw new AppException(ErrorCode.OPENAI_API_FAIL);

            completeQuizSuccess(job, article, AiJobType.QUIZ_TERM, jsonText);

            metrics.incProcessed(AiJobType.QUIZ_TERM, "success");
        } catch (AppException e) {
            markJobFailed(jobId, e.getErrorCode());
            metrics.incProcessed(AiJobType.QUIZ_TERM, "failed");
        } catch (Exception e) {
            log.error("[AI] quiz_term unexpected error jobId={}", jobId, e);
            markJobFailed(jobId, ErrorCode.OPENAI_API_FAIL);
            metrics.incProcessed(AiJobType.QUIZ_TERM, "failed");
        } finally {
            metrics.stopTimer(sample, AiJobType.QUIZ_TERM);
        }
    }

    @Transactional
    protected void completeQuizSuccess(AiJobEntity job, NaverArticleEntity article,
                                       AiJobType kind, String quizJson) {

        if (aiQuizSetRepository.existsByJobId(job.getId())) {
            job.markSuccess();
            return;
        }

        AiQuizSetEntity entity = AiQuizSetEntity.builder()
                .job(job)
                .article(article)
                .quizKind(kind)
                .quizJson(quizJson)
                .build();

        try {
            aiQuizSetRepository.save(entity);
        } catch (DataIntegrityViolationException dup) {
            // uk_ai_quiz_job 레이스 방어
        }

        job.markSuccess();
    }

    // =========================
    // 공통 로딩/의존성
    // =========================
    @Transactional(readOnly = true)
    protected AiJobEntity loadRunningJob(Long jobId, AiJobType expectedType) {
        AiJobEntity job = aiJobRepository.findByIdWithArticle(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_JOB_NOT_FOUND));

        if (job.getStatus() != AiJobStatus.RUNNING) throw new AppException(ErrorCode.BAD_REQUEST);
        if (job.getJobType() != expectedType) throw new AppException(ErrorCode.BAD_REQUEST);
        return job;
    }

    @Transactional(readOnly = true)
    protected JobAndSummary loadJobAndSummary(Long jobId, AiJobType expectedType) {
        AiJobEntity job = loadRunningJob(jobId, expectedType);
        var article = job.getArticle();

        // 가장 안전: "기사 기준 최신 summary"를 가져오기
        AiArticleSummaryEntity summary = aiArticleSummaryRepository
                .findTopByArticleIdOrderByCreatedAtDesc(article.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        String summaryFull = summary.getSummaryFull();
        if (isBlank(summaryFull)) throw new AppException(ErrorCode.NOT_FOUND);

        return new JobAndSummary(job, article, summaryFull);
    }

    @Transactional(readOnly = true)
    protected List<AiTermCardEntity> fetchTermCardsForArticle(Long articleId) {
        return aiTermCardRepository.findByArticleIdOrderByCardOrderAsc(articleId);
    }

    private String buildTermCardsText(List<AiTermCardEntity> cards) {
        StringBuilder sb = new StringBuilder();
        int i = 1;

        for (AiTermCardEntity c : cards) {
            TermEntity t = c.getTerm();
            sb.append(i++).append(") ").append(t.getDisplayName()).append("\n")
                    .append("- 정의: ").append(t.getDefinition()).append("\n")
                    .append("- 하이라이트: ").append(c.getHighlightText()).append("\n\n");
        }

        return sb.toString();
    }

    @Transactional
    protected void markJobFailed(Long jobId, ErrorCode errorCode) {
        AiJobEntity job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_JOB_NOT_FOUND));
        job.markFailed(errorCode.getCode(), errorCode.getMessage());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record JobAndSummary(
            AiJobEntity job,
            NaverArticleEntity article,
            String summaryFull
    ) {}
}
