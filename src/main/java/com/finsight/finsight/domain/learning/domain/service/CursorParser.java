package com.finsight.finsight.domain.learning.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.finsight.domain.learning.exception.LearningException;
import com.finsight.finsight.domain.learning.exception.code.LearningErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Base64;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CursorParser {

    private final ObjectMapper objectMapper;

    // 커서 데이터를 담는 내부 레코드
    public record NewsCursor(LocalDateTime lastPublishedAt, Long viewCount, Long lastId) {
        public boolean isEmpty(){
            return lastId == null;
        }
    }

    // 객체 -> Base64 문자열
    public String encode(LocalDateTime lastPublishedAt, Long viewCount, Long lastId) {
        try {
            NewsCursor cursor = new NewsCursor(lastPublishedAt, viewCount, lastId);
            String json = objectMapper.writeValueAsString(cursor);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (Exception e) {
            throw new LearningException(LearningErrorCode.CURSOR_ENCODING_ERROR);
        }
    }

    // Base64 문자열 -> 객체
    public NewsCursor decode(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) return null;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cursorStr);
            return objectMapper.readValue(new String(decodedBytes), NewsCursor.class);
        } catch (Exception e) {
            throw new LearningException(LearningErrorCode.INVALID_CURSOR_FORMAT);
        }
    }
}
