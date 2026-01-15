package com.finsight.finsight.domain.naver.domain.service;

import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.learning.domain.service.CursorParser;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.entity.QNaverArticleEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleQueryDslImpl implements ArticleQueryDsl{

    private final JPAQueryFactory queryFactory;

    // 인기순 최신순 반환 API
    @Override
    public List<NaverArticleEntity> findNewsByCondition(
            Category category,
            SortType sort,
            int size,
            CursorParser.NewsCursor cursor
    ){
        // Q클래스 선언
        QNaverArticleEntity naverArticleEntity = QNaverArticleEntity.naverArticleEntity;

        Predicate predicate = buildPredicate(naverArticleEntity, category, sort, cursor);

        return queryFactory
                .selectFrom(naverArticleEntity)
                .where(predicate)
                .orderBy(orderSpecifiers(naverArticleEntity, sort))
                .limit(size + 1L) // size+1: hasNext 판별용
                .fetch();
    }

    // predicate 제작, where 절 (카테고리별 / 페이지 별)
    private Predicate buildPredicate(
            QNaverArticleEntity naverArticleEntity,
            Category category,
            SortType sort,
            CursorParser.NewsCursor cursor
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        // 1) 카테고리 조건
        if (category != null && category != Category.ALL) {
            NaverEconomySection section = NaverEconomySection.valueOf(category.name());
            builder.and(naverArticleEntity.section.eq(section));
        }

        // 2) 커서 조건
        if (cursor != null && !cursor.isEmpty()) {
            builder.and(cursorPredicate(naverArticleEntity, sort, cursor));
        }

        // 3) 용어 카드 3개 이상 조건
        QAiTermCardEntity qAiTermCardEntity = QAiTermCardEntity.aiTermCardEntity;
        builder.and(naverArticleEntity.id.in(
                JPAExpressions.select(qAiTermCardEntity.article.id)
                        .from(qAiTermCardEntity)
                        .groupBy(qAiTermCardEntity.article.id)
                        .having(qAiTermCardEntity.count().goe(3L))));

        return builder;
    }

    // 커서 조건(최신순/인기순)
    private Predicate cursorPredicate(QNaverArticleEntity naverArticleEntity, SortType sort, CursorParser.NewsCursor cursor) {
        return switch (sort) {
            case LATEST -> (
                    naverArticleEntity.publishedAt.lt(cursor.lastPublishedAt())
                            .or(naverArticleEntity.publishedAt.eq(cursor.lastPublishedAt()).and(naverArticleEntity.id.lt(cursor.lastId())))
            );

            //TODO: view count 속성이 추가되면 인기순 로직 구현
            case POPULARITY -> (
                    naverArticleEntity.publishedAt.lt(cursor.lastPublishedAt())
                            .or(naverArticleEntity.publishedAt.eq(cursor.lastPublishedAt()).and(naverArticleEntity.id.lt(cursor.lastId())))
//                    naverArticleEntity.viewCount.lt(cursor.viewCount())
//                            .or(naverArticleEntity.viewCount.eq(cursor.viewCount()).and(naverArticleEntity.id.lt(cursor.lastId())))
            );
        };
    }

    // 정렬하기, orderBy 절 (최신순/인기순)
    private OrderSpecifier<?>[] orderSpecifiers(QNaverArticleEntity naverArticleEntity, SortType sort) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier<?>[]{
                    naverArticleEntity.publishedAt.desc(),
                    naverArticleEntity.id.desc()
            };
            case POPULARITY -> new OrderSpecifier<?>[]{
//                    a.viewCount.desc(),
//                    a.id.desc()
            };
        };
    }
}
