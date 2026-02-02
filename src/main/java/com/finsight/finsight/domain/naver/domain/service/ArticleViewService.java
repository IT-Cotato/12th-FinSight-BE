package com.finsight.finsight.domain.naver.domain.service;

import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.entity.UserArticleViewEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.domain.naver.persistence.repository.UserArticleViewRepository;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
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
    private final UserArticleViewRepository userArticleViewRepository;
    private final UserRepository userRepository;

    private static final String VIEW_KEY_PREFIX = "article:view:";

    // 조회수 증가 (Redis) + 사용자 읽음 처리
    public void incrementViewCount(Long articleId, Long userId) {
        String key = VIEW_KEY_PREFIX + articleId;
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis connection failed: {}", e.getMessage());
        }

        // 사용자 읽음 처리 (DB 바로 저장)
        if (userId != null) {
            saveUserArticleView(articleId, userId);
        }
    }

    private void saveUserArticleView(Long articleId, Long userId) {
        // 이미 읽은 기사인지 확인
        if (userArticleViewRepository.existsByUserUserIdAndArticleId(userId, articleId)) {
            return;
        }

        UserEntity user = userRepository.getReferenceById(userId);
        NaverArticleEntity article = naverArticleRepository.getReferenceById(articleId);

        UserArticleViewEntity viewEntity = UserArticleViewEntity.create(user, article);
        userArticleViewRepository.save(viewEntity);
    }

    // 5분마다 DB 동기화
    @Scheduled(fixedRate = 300000) // 5분
    @Transactional
    public void syncViewCounts() {
        log.info("[ArticleViewService] Starting sync view counts to DB...");

        try {
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
        } catch (Exception e) {
            log.error("Redis connection failed during sync: {}", e.getMessage());
        }
    }
}
