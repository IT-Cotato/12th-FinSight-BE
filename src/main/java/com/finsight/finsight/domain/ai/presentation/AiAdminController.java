package com.finsight.finsight.domain.ai.presentation;

import com.finsight.finsight.domain.ai.exception.code.AiErrorCode;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiJobStatus;
import com.finsight.finsight.domain.ai.persistence.repository.AiJobRepository;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Job 관리자 API (dev 프로필 전용)
 * - SUSPENDED 상태의 Job을 수동으로 재개
 */
@Slf4j
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/v1/admin/ai/jobs")
@Tag(name = "AI Admin", description = "AI Job 관리자 API")
public class AiAdminController {

    private static final String ADMIN_TOKEN_HEADER = "X-ADMIN-TOKEN";

    private final AiJobRepository aiJobRepository;
    private final AiAdminProperties props;

    @Operation(summary = "SUSPENDED Job 단건 재개", description = "특정 SUSPENDED Job을 PENDING으로 전환")
    @PostMapping("/{jobId}/resume")
    @Transactional
    public ResponseEntity<DataResponse<Map<String, Object>>> resumeJob(
            @PathVariable Long jobId,
            @RequestHeader(ADMIN_TOKEN_HEADER) String adminToken
    ) {
        validateAdminToken(adminToken);

        AiJobEntity job = aiJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(AiErrorCode.AI_JOB_NOT_FOUND));

        if (job.getStatus() != AiJobStatus.SUSPENDED) {
            throw new AppException(AiErrorCode.AI_JOB_NOT_SUSPENDED);
        }

        job.resume();

        log.info("[AI-ADMIN] event_type=ai_job_resumed job_id={} job_type={}",
                job.getId(), job.getJobType());

        return ResponseEntity.ok(DataResponse.from(Map.of("resumedCount", 1)));
    }

    @Operation(summary = "SUSPENDED Job 일괄 재개", description = "reason 조건에 맞는 SUSPENDED Job들을 일괄 PENDING으로 전환")
    @PostMapping("/resume")
    @Transactional
    public ResponseEntity<DataResponse<Map<String, Object>>> resumeJobsByReason(
            @RequestParam String reason,
            @RequestHeader(ADMIN_TOKEN_HEADER) String adminToken
    ) {
        validateAdminToken(adminToken);

        List<String> errorCodes = resolveErrorCodes(reason);
        List<AiJobEntity> suspendedJobs = aiJobRepository.findSuspendedJobsByErrorCodes(
                AiJobStatus.SUSPENDED, errorCodes);

        if (suspendedJobs.isEmpty()) {
            log.info("[AI-ADMIN] event_type=ai_job_resume_batch reason={} count=0", reason);
            return ResponseEntity.ok(DataResponse.from(Map.of("resumedCount", 0)));
        }

        for (AiJobEntity job : suspendedJobs) {
            job.resume();
        }

        log.info("[AI-ADMIN] event_type=ai_job_resume_batch reason={} count={}",
                reason, suspendedJobs.size());

        return ResponseEntity.ok(DataResponse.from(Map.of("resumedCount", suspendedJobs.size())));
    }

    private void validateAdminToken(String token) {
        if (token == null || !token.equals(props.getAdminToken())) {
            log.warn("[AI-ADMIN] event_type=ai_admin_token_invalid");
            throw new AppException(AiErrorCode.ADMIN_TOKEN_INVALID);
        }
    }

    /**
     * reason 파라미터에 따른 에러코드 목록 반환
     * - QUOTA: 쿼터/잔액 관련 에러코드
     * - AUTH: 인증/권한 관련 에러코드
     */
    private List<String> resolveErrorCodes(String reason) {
        return switch (reason.toUpperCase()) {
            case "QUOTA" -> List.of("AI-020", "AI-021"); // QUOTA_EXHAUSTED, INSUFFICIENT_BALANCE
            case "AUTH" -> List.of("AI-022", "AI-023");  // INVALID_API_KEY, ACCESS_DENIED
            default -> throw new AppException(AiErrorCode.OPENAI_INVALID_REQUEST);
        };
    }

    @Component
    @ConfigurationProperties(prefix = "ai")
    @Getter
    @Setter
    public static class AiAdminProperties {
        private String adminToken;
    }
}
