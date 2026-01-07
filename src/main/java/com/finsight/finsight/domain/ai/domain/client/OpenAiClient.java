package com.finsight.finsight.domain.ai.domain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.finsight.finsight.global.config.OpenAiProperties;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.ErrorCode;
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

    private final WebClient openAiWebClient;
    private final OpenAiProperties props;

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

        return callWithRetry(body);
    }

    private JsonNode callWithRetry(Map<String, Object> body) {
        int maxTries = 3;
        long baseSleepMs = 400;

        for (int attempt = 1; attempt <= maxTries; attempt++) {
            try {
                return openAiWebClient.post()
                        .uri("/responses")
                        .bodyValue(body)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, resp ->
                                resp.bodyToMono(String.class).defaultIfEmpty("")
                                        .flatMap(msg -> Mono.error(new WebClientResponseException(
                                                "OpenAI error: " + msg,
                                                resp.statusCode().value(),
                                                resp.statusCode().toString(),
                                                null, null, null
                                        )))
                        )
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofMillis(props.getTimeoutMs()));

            } catch (WebClientResponseException e) {
                int code = e.getRawStatusCode();

                if ((code == 429 || code >= 500) && attempt < maxTries) {
                    sleep(jitter(baseSleepMs * (1L << (attempt - 1))));
                    continue;
                }

                if (code == 429) throw new AppException(ErrorCode.OPENAI_RATE_LIMIT);
                throw new AppException(ErrorCode.OPENAI_API_FAIL);

            } catch (Exception e) {
                if (attempt < maxTries) {
                    sleep(jitter(baseSleepMs * (1L << (attempt - 1))));
                    continue;
                }
                throw new AppException(ErrorCode.OPENAI_API_FAIL);
            }
        }

        throw new AppException(ErrorCode.OPENAI_API_FAIL);
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

    private static long jitter(long base) {
        return base + (long) (Math.random() * 200);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
