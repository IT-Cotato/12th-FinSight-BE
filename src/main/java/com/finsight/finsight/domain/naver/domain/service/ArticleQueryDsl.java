package com.finsight.finsight.domain.naver.domain.service;

import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.learning.domain.service.CursorParser;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ArticleQueryDsl {

    // 인기순 최신순 반환 커서기반 페이지네이션 API
    List<NaverArticleEntity> findNewsByCondition(
            Category category,
            SortType sort,
            int size,
            CursorParser.NewsCursor cursor
    );

    // 페이지 기반 검색 결과
    Page<NaverArticleEntity> findSearchNews(SortType sort, int page, int size, String keyword);

    // 카테고리별 인기순 상위 N개 조회 (홈 인기뉴스용)
    List<NaverArticleEntity> findTopPopularBySection(NaverEconomySection section, int limit);
}
