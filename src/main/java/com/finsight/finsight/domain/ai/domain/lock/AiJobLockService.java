package com.finsight.finsight.domain.ai.domain.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * AI Job Redis 분산락 서비스
 *
 * - lock key: ai:job:lock:{jobId}
 * - lock value: {workerId}:{uuid}
 * - 획득: SET NX PX ttl
 * - 해제: Lua 스크립트로 value 일치 시에만 DEL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobLockService {

    private static final String LOCK_KEY_PREFIX = "ai:job:lock:";
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofMinutes(5);

    // Lua 스크립트: value가 일치할 때만 DEL
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";

    private static final DefaultRedisScript<Long> UNLOCK_REDIS_SCRIPT;
    static {
        UNLOCK_REDIS_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_REDIS_SCRIPT.setScriptText(UNLOCK_SCRIPT);
        UNLOCK_REDIS_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final String workerId = generateWorkerId();

    /**
     * 락 획득 시도
     *
     * @param jobId 잠금할 Job ID
     * @return 락 토큰 (성공 시) 또는 null (실패 시)
     */
    public String tryLock(Long jobId) {
        return tryLock(jobId, DEFAULT_LOCK_TTL);
    }

    /**
     * 락 획득 시도 (TTL 지정)
     *
     * @param jobId 잠금할 Job ID
     * @param ttl 락 유지 시간
     * @return 락 토큰 (성공 시) 또는 null (실패 시)
     */
    public String tryLock(Long jobId, Duration ttl) {
        String key = lockKey(jobId);
        String token = lockToken();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, token, ttl);

        if (Boolean.TRUE.equals(acquired)) {
            log.debug("[AI-LOCK] acquired job_id={} token={}", jobId, maskToken(token));
            return token;
        }

        log.debug("[AI-LOCK] failed to acquire job_id={}", jobId);
        return null;
    }

    /**
     * 락 해제
     *
     * @param jobId 잠금 해제할 Job ID
     * @param token 획득 시 받은 락 토큰
     * @return 해제 성공 여부
     */
    public boolean unlock(Long jobId, String token) {
        if (token == null) {
            return false;
        }

        String key = lockKey(jobId);
        Long result = redisTemplate.execute(
                UNLOCK_REDIS_SCRIPT,
                List.of(key),
                token
        );

        boolean released = result != null && result == 1L;

        if (released) {
            log.debug("[AI-LOCK] released job_id={}", jobId);
        } else {
            log.warn("[AI-LOCK] unlock failed (token mismatch or expired) job_id={}", jobId);
        }

        return released;
    }

    /**
     * 락 상태 확인 (디버깅/모니터링 용)
     */
    public boolean isLocked(Long jobId) {
        String key = lockKey(jobId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 락 TTL 연장 (장기 작업 시)
     */
    public boolean extendLock(Long jobId, String token, Duration extension) {
        String key = lockKey(jobId);
        String currentValue = redisTemplate.opsForValue().get(key);

        if (token.equals(currentValue)) {
            return Boolean.TRUE.equals(redisTemplate.expire(key, extension));
        }
        return false;
    }

    // === Private helpers ===

    private String lockKey(Long jobId) {
        return LOCK_KEY_PREFIX + jobId;
    }

    private String lockToken() {
        return workerId + ":" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String generateWorkerId() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null || hostname.isBlank()) {
            hostname = "worker";
        }
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 4);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 8) + "***";
    }
}
