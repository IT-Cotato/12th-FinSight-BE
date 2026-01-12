package com.finsight.finsight.domain.naver.application.usecase;

import com.finsight.finsight.domain.naver.domain.service.NaverCrawlerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ConditionalOnProperty(value = "naver.crawler.enabled", havingValue = "true", matchIfMissing = true)
public class NaverEconomyCrawlScheduler {

    private final NaverCrawlerService crawlerService;

    @Scheduled(cron = "${naver.crawler.cron}", zone = "Asia/Seoul")
    public void run() {
        log.info("[NAVER-CRAWL] scheduler start");
        crawlerService.crawlAllOnce();
        log.info("[NAVER-CRAWL] scheduler end");
    }
}
