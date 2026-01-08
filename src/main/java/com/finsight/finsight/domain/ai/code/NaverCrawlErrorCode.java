package com.finsight.finsight.domain.ai.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NaverCrawlErrorCode implements BaseErrorCode {

    // 502/503 성격(외부 의존 실패)
    NAVER_LIST_FETCH_FAIL(HttpStatus.BAD_GATEWAY, "네이버 목록 페이지 조회 실패", "NAVER-001"),
    NAVER_ARTICLE_FETCH_FAIL(HttpStatus.BAD_GATEWAY, "네이버 기사 페이지 조회 실패", "NAVER-002"),
    NAVER_ARTICLE_PARSE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 기사 파싱 실패", "NAVER-003")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
