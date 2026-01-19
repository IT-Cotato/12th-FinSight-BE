    package com.finsight.finsight.domain.naver.domain.service;

    import com.finsight.finsight.domain.ai.persistence.entity.QAiArticleSummaryEntity;
    import com.finsight.finsight.domain.learning.domain.constant.Category;
    import com.finsight.finsight.domain.learning.domain.constant.SortType;
    import com.finsight.finsight.domain.learning.domain.service.CursorParser;
    import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
    import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
    import com.finsight.finsight.domain.naver.persistence.entity.QNaverArticleEntity;
    import com.finsight.finsight.domain.ai.persistence.entity.QAiTermCardEntity;
    import com.querydsl.core.BooleanBuilder;
    import com.querydsl.core.types.OrderSpecifier;
    import com.querydsl.core.types.Predicate;
    import com.querydsl.jpa.JPAExpressions;
    import com.querydsl.jpa.impl.JPAQueryFactory;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageImpl;
    import org.springframework.data.domain.PageRequest;
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

        @Override
        public Page<NaverArticleEntity> findSearchNews(SortType sort, int page, int size, String keyword){

            // Q클래스 선언
            QNaverArticleEntity naverArticleEntity = QNaverArticleEntity.naverArticleEntity;

            BooleanBuilder builder = new BooleanBuilder();

            if (keyword != null && !keyword.isBlank()) {
                QAiTermCardEntity qAiTermCard = QAiTermCardEntity.aiTermCardEntity;
                QAiArticleSummaryEntity qSummary = QAiArticleSummaryEntity.aiArticleSummaryEntity;

                // 제목에 포함되는 경우
                builder.and(naverArticleEntity.title.contains(keyword)
                        .or(JPAExpressions.selectOne() // 2. 용어 카드들 중에 키워드를 포함하는 용어가 하나라도 존재하는 경우
                                .from(qAiTermCard)
                                .where(
                                        qAiTermCard.article.eq(naverArticleEntity) // 현재 기사와 연결된 용어 중
                                                .and(qAiTermCard.term.displayName.contains(keyword)) // 용어명이 키워드를 포함
                                ).exists())
                        .or(JPAExpressions.selectOne() // 3. 본문/요약 검색 (exists)
                                .from(qSummary)
                                .where(qSummary.article.eq(naverArticleEntity)
                                        .and(qSummary.summaryFull.contains(keyword) // 전체 요약 검색
                                                .or(qSummary.summary3Lines.contains(keyword)))) // 3줄 요약 검색
                                .exists()));
            }

            // 용어 카드 3개 이상 조건
            QAiTermCardEntity qAiTermCardEntity = QAiTermCardEntity.aiTermCardEntity;
            builder.and(naverArticleEntity.id.in(
                    JPAExpressions.select(qAiTermCardEntity.article.id)
                            .from(qAiTermCardEntity)
                            .groupBy(qAiTermCardEntity.article.id)
                            .having(qAiTermCardEntity.count().goe(3L))));

            // 2. 콘텐츠 조회 (Offset 기반)
            List<NaverArticleEntity> content = queryFactory
                    .selectFrom(naverArticleEntity)
                    .where(builder)
                    .orderBy(orderSpecifiers(naverArticleEntity, sort))
                    .offset((long) page * size)
                    .limit(size)
                    .fetch();

            // 3. 전체 개수 조회
            Long total = queryFactory
                    .select(naverArticleEntity.count())
                    .from(naverArticleEntity)
                    .where(builder)
                    .fetchOne();

            long totalCount = (total != null) ? total : 0L;

            // 4. PageImpl 객체로 감싸서 반환
            return new PageImpl<>(content, PageRequest.of(page, size), totalCount);


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

                case POPULARITY -> (
                        naverArticleEntity.viewCount.lt(cursor.viewCount())
                                .or(naverArticleEntity.viewCount.eq(cursor.viewCount()).and(naverArticleEntity.id.lt(cursor.lastId())))
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
                        naverArticleEntity.viewCount.desc(),
                        naverArticleEntity.id.desc()
                };
            };
        }
    }
