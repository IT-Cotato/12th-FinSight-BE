package com.finsight.finsight.domain.ai.domain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.finsight.domain.ai.exception.code.AiErrorCode;
import com.finsight.finsight.global.config.OpenAiProperties;
import com.finsight.finsight.global.exception.AppException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final ObjectMapper OM = new ObjectMapper();

    private final WebClient openAiWebClient;
    private final OpenAiProperties props;
    private final MeterRegistry meterRegistry;

    public JsonNode createJsonSchemaResponse(List<Map<String, String>> input, String schemaName, JsonNode jsonSchema) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("input", input);
        body.put("temperature", props.getTemperature());
        body.put("max_output_tokens", props.getMaxOutputTokens());
        body.put("truncation", "auto");
        body.put("store", false);
        body.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", schemaName,
                        "strict", true,
                        "schema", jsonSchema
                )
        ));

        return callApi(body, schemaName);
    }

    /**
     * API 호출 (재시도 없음 - 재시도는 Job 레벨에서 처리)
     */
    private JsonNode callApi(Map<String, Object> body, String schemaName) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            JsonNode result = openAiWebClient.post()
                    .uri("/responses")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofMillis(props.getTimeoutMs()));

            // 성공 메트릭
            long latencyMs = recordLatency(sample, schemaName, "success");
            incApiCounter(schemaName, "success", null);

            // 성공 로그 (토큰 정보 포함)
            int totalTokens = extractTotalTokens(result);
            log.info("[OPENAI] event_type=api_success schema={} latency_ms={} total_tokens={}",
                    schemaName, latencyMs, totalTokens);

            return result;

        } catch (OpenAiApiException e) {
            recordLatency(sample, schemaName, "error");
            AiErrorCode errorCode = e.getErrorCode();

            // 구조화 로그 (키/프롬프트 제외)
            log.warn("[OPENAI] event_type=ai_api_error schema={} http_status={} error_code={} is_retryable={}",
                    schemaName, e.getHttpStatus(), errorCode.getCode(), errorCode.isRetryable());

            incApiCounter(schemaName, "error", errorCode.getCode());
            throw new AppException(errorCode);

        } catch (IllegalStateException e) {
            // Timeout
            recordLatency(sample, schemaName, "timeout");
            log.warn("[OPENAI] event_type=ai_api_timeout schema={} timeout_ms={}", schemaName, props.getTimeoutMs());

            incApiCounter(schemaName, "timeout", AiErrorCode.OPENAI_TIMEOUT.getCode());
            throw new AppException(AiErrorCode.OPENAI_TIMEOUT);

        } catch (AppException e) {
            throw e;

        } catch (Exception e) {
            recordLatency(sample, schemaName, "error");
            log.error("[OPENAI] event_type=ai_api_unexpected schema={}", schemaName, e);

            incApiCounter(schemaName, "error", AiErrorCode.OPENAI_API_FAIL.getCode());
            throw new AppException(AiErrorCode.OPENAI_API_FAIL);
        }
    }

    private Mono<Throwable> handleErrorResponse(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    int status = response.statusCode().value();
                    AiErrorCode errorCode = parseErrorCode(status, body);
                    return new OpenAiApiException(status, errorCode, body);
                });
    }

    /**
     * HTTP 상태 코드와 응답 본문으로 AiErrorCode 결정
     */
    private AiErrorCode parseErrorCode(int httpStatus, String responseBody) {
        String errorCode = extractErrorCode(responseBody);

        // 에러 코드 기반 매핑 (우선)
        if (errorCode != null) {
            String lowerCode = errorCode.toLowerCase();
            if (lowerCode.contains("insufficient_quota")) {
                return AiErrorCode.OPENAI_QUOTA_EXHAUSTED;
            }
            if (lowerCode.contains("insufficient_balance")) {
                return AiErrorCode.OPENAI_INSUFFICIENT_BALANCE;
            }
            if (lowerCode.contains("invalid_api_key")) {
                return AiErrorCode.OPENAI_INVALID_API_KEY;
            }
            if (lowerCode.contains("rate_limit")) {
                return AiErrorCode.OPENAI_RATE_LIMIT;
            }
        }

        // HTTP 상태 코드 기반 매핑
        return switch (httpStatus) {
            case 401 -> AiErrorCode.OPENAI_INVALID_API_KEY;
            case 402 -> AiErrorCode.OPENAI_INSUFFICIENT_BALANCE;
            case 403 -> AiErrorCode.OPENAI_ACCESS_DENIED;
            case 400 -> AiErrorCode.OPENAI_INVALID_REQUEST;
            case 429 -> AiErrorCode.OPENAI_RATE_LIMIT;
            default -> {
                if (httpStatus >= 500) yield AiErrorCode.OPENAI_SERVER_ERROR;
                yield AiErrorCode.OPENAI_API_FAIL;
            }
        };
    }

    private String extractErrorCode(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            JsonNode root = OM.readTree(responseBody);
            JsonNode error = root.path("error");
            String code = error.path("code").asText(null);
            if (code != null) return code;
            return error.path("type").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    // === 메트릭 헬퍼 ===

    private void incApiCounter(String schema, String status, String errorCode) {
        Counter.Builder builder = Counter.builder("openai_api_requests_total")
                .tag("model", props.getModel())
                .tag("schema", schema)
                .tag("status", status);

        if (errorCode != null) {
            builder.tag("error_code", errorCode);
        }

        builder.register(meterRegistry).increment();
    }

    private long recordLatency(Timer.Sample sample, String schema, String status) {
        long nanos = sample.stop(Timer.builder("openai_api_latency_seconds")
                .tag("model", props.getModel())
                .tag("schema", schema)
                .tag("status", status)
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(meterRegistry));
        return nanos / 1_000_000; // nanoseconds to milliseconds
    }

    private int extractTotalTokens(JsonNode result) {
        if (result == null) return 0;
        JsonNode usage = result.path("usage");
        return usage.path("total_tokens").asInt(0);
    }

    public static String extractOutputText(JsonNode root) {
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) continue;
                for (JsonNode c : content) {
                    if ("output_text".equals(c.path("type").asText())) {
                        String t = c.path("text").asText(null);
                        if (t != null && !t.isBlank()) return t;
                    }
                }
            }
        }
        return root.path("output_text").asText(null);
    }

    // === 내부 예외 클래스 ===

    private static class OpenAiApiException extends RuntimeException {
        private final int httpStatus;
        private final AiErrorCode errorCode;

        OpenAiApiException(int httpStatus, AiErrorCode errorCode, String responseBody) {
            super("OpenAI API error: " + errorCode.getCode());
            this.httpStatus = httpStatus;
            this.errorCode = errorCode;
        }

        int getHttpStatus() {
            return httpStatus;
        }

        AiErrorCode getErrorCode() {
            return errorCode;
        }
    }
}
