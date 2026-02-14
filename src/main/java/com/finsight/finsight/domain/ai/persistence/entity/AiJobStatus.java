package com.finsight.finsight.domain.ai.persistence.entity;

public enum AiJobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    /** 재시도 대기 (nextRunAt 이후 PENDING으로 전환) */
    RETRY_WAIT,
    /** 수동 확인 필요 (쿼터 소진 등 자동 복구 불가) */
    SUSPENDED
}

