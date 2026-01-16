package com.finsight.finsight.domain.naver.exception.code;

import com.finsight.finsight.global.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NaverArticleErrorCode implements BaseErrorCode {

    NAVER_ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE-404-0", "id에 해당하는 뉴스 기사가 없습니다."),
    NAVER_ARTICLE_SUMMERY_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE-404-1", "id에 해당하는 요약이 없습니다."),
    NAVER_ARTICLE_INSIGHT_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE-404-2", "id에 해당하는 insight가 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
