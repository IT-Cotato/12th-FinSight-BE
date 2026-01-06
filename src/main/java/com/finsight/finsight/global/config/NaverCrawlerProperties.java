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
}
