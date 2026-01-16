package com.finsight.finsight.domain.naver.domain.service;

import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NaverArticleRepository naverArticleRepository;

    private static final String VIEW_KEY_PREFIX = "article:view:";

    // 조회수 증가 (Redis)
    public void incrementViewCount(Long articleId) {
        String key = VIEW_KEY_PREFIX + articleId;
        redisTemplate.opsForValue().increment(key);
    }

    // 5분마다 DB 동기화
    @Scheduled(fixedRate = 300000) // 5분
    @Transactional
    public void syncViewCounts() {
        log.info("[ArticleViewService] Starting sync view counts to DB...");

        Set<String> keys = redisTemplate.keys(VIEW_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            String idStr = key.substring(VIEW_KEY_PREFIX.length());
            Long articleId = Long.parseLong(idStr);

            String countStr = (String) redisTemplate.opsForValue().get(key);
            if (countStr != null) {
                long count = Long.parseLong(countStr);

                // DB 업데이트
                naverArticleRepository.findById(articleId).ifPresent(article -> {
                    article.increaseViewCount(count);
                });

                // Redis 키 삭제 (또는 decrement로 처리할 수도 있으나, 여기서는 단순 합산 후 리셋 방식 사용)
                redisTemplate.delete(key);
            }
        }

        log.info("[ArticleViewService] Finished sync view counts. Processed keys: {}", keys.size());
    }
}
