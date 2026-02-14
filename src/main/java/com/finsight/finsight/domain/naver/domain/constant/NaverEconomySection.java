package com.finsight.finsight.domain.naver.domain.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NaverEconomySection {

    FINANCE("금융", "https://news.naver.com/breakingnews/section/101/259"),
    STOCK("증권", "https://news.naver.com/breakingnews/section/101/258"),
    INDUSTRY("산업/재계", "https://news.naver.com/breakingnews/section/101/261"),
    SME("중기/벤처", "https://news.naver.com/breakingnews/section/101/771"),
    REAL_ESTATE("부동산", "https://news.naver.com/breakingnews/section/101/260"),
    GLOBAL("글로벌 경제", "https://news.naver.com/breakingnews/section/101/262"),
    LIVING("생활경제", "https://news.naver.com/breakingnews/section/101/310"),
    GENERAL("경제 일반", "https://news.naver.com/breakingnews/section/101/263");

    private final String displayName;
    private final String url;
}
