package com.finsight.finsight.domain.ai.persistence.entity;

public enum AiJobType {
    SUMMARY,          // 3줄/전체 요약
    INSIGHT,          // 인사이트
    TERM_CARDS,       // 핵심용어 3개(+해설+하이라이트)
    QUIZ_CONTENT,     // 내용 퀴즈
    QUIZ_TERM         // 용어 퀴즈
}
