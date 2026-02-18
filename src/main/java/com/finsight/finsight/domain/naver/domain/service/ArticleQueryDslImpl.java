package com.finsight.finsight.domain.naver.domain.service;

import com.finsight.finsight.domain.ai.persistence.entity.AiJobType;
import com.finsight.finsight.domain.ai.persistence.entity.QAiArticleSummaryEntity;
import com.finsight.finsight.domain.ai.persistence.entity.QAiQuizSetEntity;
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
public class ArticleQueryDslImpl implements ArticleQueryDsl {

        private final JPAQueryFactory queryFactory;

        // 인기순 최신순 반환 API
        @Override
        public List<NaverArticleEntity> findNewsByCondition(
                        Category category,
                        SortType sort,
                        int size,
                        CursorParser.NewsCursor cursor) {
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
        public Page<NaverArticleEntity> findSearchNews(Category category, SortType sort, int page, int size,
                        String keyword) {

                // Q클래스 선언
                QNaverArticleEntity naverArticleEntity = QNaverArticleEntity.naverArticleEntity;

                BooleanBuilder builder = new BooleanBuilder();

                // 1) 카테고리 조건
                if (category != null && category != Category.ALL) {
                        NaverEconomySection section = NaverEconomySection.valueOf(category.name());
                        builder.and(naverArticleEntity.section.eq(section));
                }

                if (keyword != null && !keyword.isBlank()) {
                        QAiTermCardEntity qAiTermCard = QAiTermCardEntity.aiTermCardEntity;
                        QAiArticleSummaryEntity qSummary = QAiArticleSummaryEntity.aiArticleSummaryEntity;

                        // 제목에 포함되는 경우 또는 용어 카드 포함 또는 요약문 포함 (대소문자 무시)
                        builder.and(naverArticleEntity.title.containsIgnoreCase(keyword)
                                        .or(JPAExpressions.selectOne()
                                                        .from(qAiTermCard)
                                                        .where(
                                                                        qAiTermCard.article.eq(naverArticleEntity)
                                                                                        .and(qAiTermCard.term.displayName
                                                                                                        .containsIgnoreCase(keyword)))
                                                        .exists())
                                        .or(JPAExpressions.selectOne()
                                                        .from(qSummary)
                                                        .where(qSummary.article.eq(naverArticleEntity)
                                                                        .and(qSummary.summaryFull.containsIgnoreCase(keyword)
                                                                                        .or(qSummary.summary3Lines
                                                                                                        .containsIgnoreCase(keyword))))
                                                        .exists()));
                }

                // 용어 카드 3개 이상 조건
                QAiTermCardEntity qAiTermCardEntity = QAiTermCardEntity.aiTermCardEntity;
                builder.and(naverArticleEntity.id.in(
                                JPAExpressions.select(qAiTermCardEntity.article.id)
                                                .from(qAiTermCardEntity)
                                                .groupBy(qAiTermCardEntity.article.id)
                                                .having(qAiTermCardEntity.count().goe(3L))));

                // 퀴즈 생성 여부 (내용 퀴즈 & 용어 퀴즈)
                QAiQuizSetEntity qAiQuizSetEntity = QAiQuizSetEntity.aiQuizSetEntity;
                builder.and(JPAExpressions.selectOne()
                                .from(qAiQuizSetEntity)
                                .where(qAiQuizSetEntity.article.eq(naverArticleEntity)
                                                .and(qAiQuizSetEntity.quizKind.eq(AiJobType.QUIZ_CONTENT)))
                                .exists());

                builder.and(JPAExpressions.selectOne()
                                .from(qAiQuizSetEntity)
                                .where(qAiQuizSetEntity.article.eq(naverArticleEntity)
                                                .and(qAiQuizSetEntity.quizKind.eq(AiJobType.QUIZ_TERM)))
                                .exists());

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
                        CursorParser.NewsCursor cursor) {
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

                // 4) 퀴즈 생성 여부 (내용 퀴즈 & 용어 퀴즈)
                QAiQuizSetEntity qAiQuizSetEntity = QAiQuizSetEntity.aiQuizSetEntity;
                builder.and(JPAExpressions.selectOne()
                                .from(qAiQuizSetEntity)
                                .where(qAiQuizSetEntity.article.eq(naverArticleEntity)
                                                .and(qAiQuizSetEntity.quizKind.eq(AiJobType.QUIZ_CONTENT)))
                                .exists());

                builder.and(JPAExpressions.selectOne()
                                .from(qAiQuizSetEntity)
                                .where(qAiQuizSetEntity.article.eq(naverArticleEntity)
                                                .and(qAiQuizSetEntity.quizKind.eq(AiJobType.QUIZ_TERM)))
                                .exists());

                return builder;
        }

        // 커서 조건(최신순/인기순)
        private Predicate cursorPredicate(QNaverArticleEntity naverArticleEntity, SortType sort,
                        CursorParser.NewsCursor cursor) {
                return switch (sort) {
                        case LATEST -> (naverArticleEntity.publishedAt.lt(cursor.lastPublishedAt())
                                        .or(naverArticleEntity.publishedAt.eq(cursor.lastPublishedAt())
                                                        .and(naverArticleEntity.id.lt(cursor.lastId()))));

                        case POPULARITY -> (naverArticleEntity.viewCount.lt(cursor.viewCount())
                                        .or(naverArticleEntity.viewCount.eq(cursor.viewCount())
                                                        .and(naverArticleEntity.id.lt(cursor.lastId()))));
                };
        }

        // 정렬하기, orderBy 절 (최신순/인기순)
        private OrderSpecifier<?>[] orderSpecifiers(QNaverArticleEntity naverArticleEntity, SortType sort) {
                return switch (sort) {
                        case LATEST -> new OrderSpecifier<?>[] {
                                        naverArticleEntity.publishedAt.desc(),
                                        naverArticleEntity.id.desc()
                        };
                        case POPULARITY -> new OrderSpecifier<?>[] {
                                        naverArticleEntity.viewCount.desc(),
                                        naverArticleEntity.id.desc()
                        };
                };
        }

        // 카테고리별 인기순 상위 N개 조회 (홈 인기뉴스용)
        @Override
        public List<NaverArticleEntity> findTopPopularBySection(NaverEconomySection section, int limit) {
                QNaverArticleEntity article = QNaverArticleEntity.naverArticleEntity;
                QAiTermCardEntity termCard = QAiTermCardEntity.aiTermCardEntity;
                QAiQuizSetEntity quizSet = QAiQuizSetEntity.aiQuizSetEntity;

                BooleanBuilder builder = new BooleanBuilder();

                // 1) 섹션 조건
                builder.and(article.section.eq(section));

                // 2) 용어 카드 3개 이상 조건
                builder.and(article.id.in(
                                JPAExpressions.select(termCard.article.id)
                                                .from(termCard)
                                                .groupBy(termCard.article.id)
                                                .having(termCard.count().goe(3L))));

                // 3) 퀴즈 생성 여부 (내용 퀴즈 & 용어 퀴즈)
                builder.and(JPAExpressions.selectOne()
                                .from(quizSet)
                                .where(quizSet.article.eq(article)
                                                .and(quizSet.quizKind.eq(AiJobType.QUIZ_CONTENT)))
                                .exists());

                builder.and(JPAExpressions.selectOne()
                                .from(quizSet)
                                .where(quizSet.article.eq(article)
                                                .and(quizSet.quizKind.eq(AiJobType.QUIZ_TERM)))
                                .exists());

                return queryFactory
                                .selectFrom(article)
                                .where(builder)
                                .orderBy(article.viewCount.desc(), article.id.desc())
                                .limit(limit)
                                .fetch();
        }

        // 카테고리별 최신순 상위 N개 조회 (홈 맞춤뉴스용)
        @Override
        public List<NaverArticleEntity> findTopLatestBySection(NaverEconomySection section, int limit) {
                QNaverArticleEntity article = QNaverArticleEntity.naverArticleEntity;
                QAiTermCardEntity termCard = QAiTermCardEntity.aiTermCardEntity;
                QAiQuizSetEntity quizSet = QAiQuizSetEntity.aiQuizSetEntity;

                BooleanBuilder builder = buildAiCompletedCondition(article, termCard, quizSet);

                // 섹션 조건
                builder.and(article.section.eq(section));

                return queryFactory
                                .selectFrom(article)
                                .where(builder)
                                .orderBy(article.publishedAt.desc(), article.id.desc())
                                .limit(limit)
                                .fetch();
        }

        // 전체 카테고리 최신순 상위 N개 조회 (홈 맞춤뉴스용)
        @Override
        public List<NaverArticleEntity> findTopLatestAll(int limit) {
                QNaverArticleEntity article = QNaverArticleEntity.naverArticleEntity;
                QAiTermCardEntity termCard = QAiTermCardEntity.aiTermCardEntity;
                QAiQuizSetEntity quizSet = QAiQuizSetEntity.aiQuizSetEntity;

                BooleanBuilder builder = buildAiCompletedCondition(article, termCard, quizSet);

                return queryFactory
                                .selectFrom(article)
                                .where(builder)
                                .orderBy(article.publishedAt.desc(), article.id.desc())
                                .limit(limit)
                                .fetch();
        }

        // AI Job 완료 조건 빌더 (용어카드 3개 이상, 퀴즈 2종 존재)
        private BooleanBuilder buildAiCompletedCondition(
                        QNaverArticleEntity article,
                        QAiTermCardEntity termCard,
                        QAiQuizSetEntity quizSet) {
                BooleanBuilder builder = new BooleanBuilder();

                // 용어 카드 3개 이상 조건
                builder.and(article.id.in(
                                JPAExpressions.select(termCard.article.id)
                                                .from(termCard)
                                                .groupBy(termCard.article.id)
                                                .having(termCard.count().goe(3L))));

                // 퀴즈 생성 여부 (내용 퀴즈 & 용어 퀴즈)
                builder.and(JPAExpressions.selectOne()
                                .from(quizSet)
                                .where(quizSet.article.eq(article)
                                                .and(quizSet.quizKind.eq(AiJobType.QUIZ_CONTENT)))
                                .exists());

                builder.and(JPAExpressions.selectOne()
                                .from(quizSet)
                                .where(quizSet.article.eq(article)
                                                .and(quizSet.quizKind.eq(AiJobType.QUIZ_TERM)))
                                .exists());

                return builder;
        }
}