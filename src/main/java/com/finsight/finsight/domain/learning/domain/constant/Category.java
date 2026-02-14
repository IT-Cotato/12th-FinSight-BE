package com.finsight.finsight.domain.learning.domain.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum Category {
    ALL("종합"),
    FINANCE("금융"),
    STOCK("증권"),
    INDUSTRY("산업/재계"),
    SME("중기/벤처"),
    REAL_ESTATE("부동산"),
    GLOBAL("글로벌 경제"),
    LIVING("생활경제"),
    GENERAL("경제 일반");

    private final String displayName;
}
