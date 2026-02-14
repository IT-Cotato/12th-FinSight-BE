package com.finsight.finsight.domain.learning.domain.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SortType {
    LATEST("최신순"),
    POPULARITY("인기순");

    private final String displayName;
}
