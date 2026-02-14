package com.finsight.finsight.domain.quiz.presentation;

import com.finsight.finsight.domain.quiz.application.dto.request.QuizSubmitRequest;
import com.finsight.finsight.domain.quiz.application.dto.response.QuizResponse;
import com.finsight.finsight.domain.quiz.application.dto.response.QuizSubmitResponse;
import com.finsight.finsight.domain.quiz.domain.service.QuizService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈 API")
public class QuizController {

    private final QuizService quizService;

    /**
     * 퀴즈 조회
     * GET /api/quiz/{naverArticleId}?type=CONTENT
     */
    @GetMapping("/{naverArticleId}")
    @Operation(summary = "퀴즈 조회")
    public ResponseEntity<DataResponse<QuizResponse>> getQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long naverArticleId,
            @RequestParam String type
    ) {
        QuizResponse response = quizService.getQuiz(
                userDetails.getUserId(), naverArticleId, type);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    /**
     * 퀴즈 제출
     * POST /api/quiz/submit
     */
    @PostMapping("/submit")
    @Operation(summary = "퀴즈 제출")
    public ResponseEntity<DataResponse<QuizSubmitResponse>> submitQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QuizSubmitRequest request
    ) {
        QuizSubmitResponse response = quizService.submitQuiz(
                userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
