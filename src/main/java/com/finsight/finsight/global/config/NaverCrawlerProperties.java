package com.finsight.finsight.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "naver.crawler")
public class NaverCrawlerProperties {
    private String cron = "0 */5 * * * *";
    private int maxPages = 2;
    private int stopAfterSeenStreak = 8;
    private int timeoutMs = 8000;
    private int sleepMinMs = 250;
    private int sleepMaxMs = 700;
    private String userAgent = "Mozilla/5.0";

    /**
     * AI 작업을 수행할 최소 본문 길이 (문자 수).
     * 이 길이 미만의 본문은 AI 요약/인사이트/퀴즈 생성을 건너뜁니다.
     * 0 이하 값은 제한 없음을 의미합니다.
     */
    private int minContentLengthForAi = 200;
}
