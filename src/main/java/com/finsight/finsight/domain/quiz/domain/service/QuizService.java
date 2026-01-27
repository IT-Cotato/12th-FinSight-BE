package com.finsight.finsight.domain.quiz.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import com.finsight.finsight.domain.ai.persistence.entity.AiQuizSetEntity;
import com.finsight.finsight.domain.ai.persistence.repository.AiQuizSetRepository;
import com.finsight.finsight.domain.quiz.application.dto.request.QuizSubmitRequest;
import com.finsight.finsight.domain.quiz.application.dto.response.QuizResponse;
import com.finsight.finsight.domain.quiz.application.dto.response.QuizSubmitResponse;
import com.finsight.finsight.domain.quiz.exception.QuizException;
import com.finsight.finsight.domain.quiz.exception.code.QuizErrorCode;
import com.finsight.finsight.domain.quiz.persistence.entity.QuizAttemptEntity;
import com.finsight.finsight.domain.quiz.persistence.repository.QuizAttemptRepository;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final AiQuizSetRepository aiQuizSetRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final int SCORE_PER_QUESTION = 10;  // 문항당 점수
    private static final int BONUS_ALL_CORRECT = 20;   // 올클리어 보너스

    /**
     * 퀴즈 조회
     */
    public QuizResponse getQuiz(Long userId, Long naverArticleId, String quizType) {
        AiJobType jobType = parseQuizType(quizType);

        // 퀴즈 세트 조회
        AiQuizSetEntity quizSet = aiQuizSetRepository
                .findTopByArticleIdAndQuizKindOrderByCreatedAtDesc(naverArticleId, jobType)
                .orElseThrow(() -> new QuizException(QuizErrorCode.QUIZ_NOT_FOUND));

        List<JsonNode> questions = parseQuizJson(quizSet.getQuizJson());

        // 직전 풀이 기록 조회
        Optional<QuizAttemptEntity> previousAttempt = 
                quizAttemptRepository.findByUserUserIdAndQuizSetId(userId, quizSet.getId());

        // 직전 정오답 맵 생성
        Map<Integer, Boolean> previousResults = new HashMap<>();
        if (previousAttempt.isPresent()) {
            previousResults = parsePreviousAnswers(previousAttempt.get().getAnswersJson());
        }

        // 응답 생성 (정답 제외)
        List<QuizResponse.QuestionItem> questionItems = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            JsonNode q = questions.get(i);
            
            List<String> options = new ArrayList<>();
            q.get("options").forEach(opt -> options.add(opt.asText()));

            questionItems.add(new QuizResponse.QuestionItem(
                    i,
                    q.get("question").asText(),
                    options,
                    previousResults.get(i)
            ));
        }

        return new QuizResponse(naverArticleId, quizType, questionItems);
    }

    /**
     * 퀴즈 제출 (채점)
     */
    @Transactional
    public QuizSubmitResponse submitQuiz(Long userId, QuizSubmitRequest request) {
        AiJobType jobType = parseQuizType(request.quizType());

        // 퀴즈 세트 조회
        AiQuizSetEntity quizSet = aiQuizSetRepository
                .findTopByArticleIdAndQuizKindOrderByCreatedAtDesc(request.naverArticleId(), jobType)
                .orElseThrow(() -> new QuizException(QuizErrorCode.QUIZ_NOT_FOUND));

        // 사용자 조회
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new QuizException(QuizErrorCode.USER_NOT_FOUND));

        List<JsonNode> questions = parseQuizJson(quizSet.getQuizJson());

        // 답안 검증
        if (request.answers() == null || request.answers().isEmpty()) {
            throw new QuizException(QuizErrorCode.INVALID_ANSWER);
        }

        // 채점
        List<QuizSubmitResponse.QuestionResult> results = new ArrayList<>();
        List<Map<String, Object>> answersForJson = new ArrayList<>();
        int correctCount = 0;

        for (QuizSubmitRequest.AnswerItem answer : request.answers()) {
            int idx = answer.questionIndex();
            int selected = answer.selectedIndex();

            // 인덱스 범위 검증
            if (idx < 0 || idx >= questions.size() || selected < 0 || selected > 3) {
                throw new QuizException(QuizErrorCode.INVALID_ANSWER);
            }

            JsonNode q = questions.get(idx);
            int answerIndex = q.get("answerIndex").asInt();
            boolean correct = (selected == answerIndex);

            if (correct) correctCount++;

            // 선택지 파싱
            List<String> options = new ArrayList<>();
            q.get("options").forEach(opt -> options.add(opt.asText()));

            // 해설 파싱
            List<String> explanations = new ArrayList<>();
            q.get("explanations").forEach(exp -> explanations.add(exp.asText()));

            results.add(new QuizSubmitResponse.QuestionResult(
                    idx, correct, selected, answerIndex,
                    q.get("question").asText(), options, explanations
            ));

            // 저장용 데이터
            Map<String, Object> answerMap = new HashMap<>();
            answerMap.put("questionIndex", idx);
            answerMap.put("selectedIndex", selected);
            answerMap.put("correct", correct);
            answersForJson.add(answerMap);
        }

        // 점수 계산
        int setScore = correctCount * SCORE_PER_QUESTION;
        if (correctCount == questions.size()) {
            setScore += BONUS_ALL_CORRECT;
        }

        // 풀이 기록 저장 (있으면 update, 없으면 insert)
        String answersJson = toJson(answersForJson);
        Optional<QuizAttemptEntity> existingAttempt = 
                quizAttemptRepository.findByUserUserIdAndQuizSetId(userId, quizSet.getId());

        if (existingAttempt.isPresent()) {
            existingAttempt.get().updateAttempt(answersJson, correctCount, setScore);
        } else {
            quizAttemptRepository.save(QuizAttemptEntity.builder()
                    .user(user)
                    .quizSet(quizSet)
                    .answersJson(answersJson)
                    .correctCount(correctCount)
                    .score(setScore)
                    .build());
        }

        // 경험치 추가
        user.addExp(setScore);

        return new QuizSubmitResponse(
                correctCount, setScore, user.getExp(), user.getLevel(), results
        );
    }

    /** quizType 문자열을 AiJobType으로 변환 */
    private AiJobType parseQuizType(String quizType) {
        return switch (quizType.toUpperCase()) {
            case "CONTENT" -> AiJobType.QUIZ_CONTENT;
            case "TERM" -> AiJobType.QUIZ_TERM;
            default -> throw new QuizException(QuizErrorCode.INVALID_QUIZ_TYPE);
        };
    }

    /** quiz_json 파싱 */
    private List<JsonNode> parseQuizJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<JsonNode> questions = new ArrayList<>();
            root.get("questions").forEach(questions::add);
            return questions;
        } catch (JsonProcessingException e) {
            throw new QuizException(QuizErrorCode.QUIZ_NOT_FOUND);
        }
    }

    /** 직전 풀이 답안 파싱 */
    private Map<Integer, Boolean> parsePreviousAnswers(String answersJson) {
        try {
            List<Map<String, Object>> answers = objectMapper.readValue(
                    answersJson, new TypeReference<>() {});
            
            Map<Integer, Boolean> result = new HashMap<>();
            for (Map<String, Object> answer : answers) {
                result.put((Integer) answer.get("questionIndex"), 
                           (Boolean) answer.get("correct"));
            }
            return result;
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    /** 객체를 JSON 문자열로 변환 */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
