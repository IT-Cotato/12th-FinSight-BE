package com.finsight.finsight.domain.learning.domain.service;

import com.finsight.finsight.domain.ai.persistence.entity.AiArticleInsightEntity;
import com.finsight.finsight.domain.ai.persistence.entity.AiArticleSummaryEntity;
import com.finsight.finsight.domain.ai.persistence.repository.AiArticleInsightRepository;
import com.finsight.finsight.domain.ai.persistence.repository.AiArticleSummaryRepository;
import com.finsight.finsight.domain.learning.application.dto.response.LearningResponseDTO;
import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.learning.persistence.mapper.LearningConverter;
import com.finsight.finsight.domain.naver.exception.NaverArticleException;
import com.finsight.finsight.domain.naver.exception.NaverCrawlException;
import com.finsight.finsight.domain.naver.exception.code.NaverArticleErrorCode;
import com.finsight.finsight.domain.naver.exception.code.NaverCrawlErrorCode;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;
import com.finsight.finsight.domain.ai.persistence.repository.AiTermCardRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class NewsQueryService {

    private final NaverArticleRepository naverArticleRepository;
    private final AiTermCardRepository aiTermCardRepository;
    private final CursorParser cursorParser;
    private final AiArticleInsightRepository aiArticleInsightRepository;
    private final AiArticleSummaryRepository aiArticleSummaryRepository;
    private final LearningConverter learningConverter;


    // 일반 뉴스 리스트 조회
    public LearningResponseDTO.NewListResponse getNewsList(Category category, SortType sort, int size, String cursorStr) {
        return getProcessedNewsResponse(category, sort, size, cursorStr, null);
    }

    // 검색 뉴스 리스트 조회
    public LearningResponseDTO.NewListResponse searchNews(String keyword, SortType sort, int size, String cursorStr) {
        return getProcessedNewsResponse(Category.ALL, sort, size, cursorStr, keyword);
    }

    // 뉴스 리스트 반환 프로세스
    public LearningResponseDTO.NewListResponse getProcessedNewsResponse(Category category, SortType sort, int size,
            String cursorStr, String keyword) {
        // 1. 커서 디코딩
        CursorParser.NewsCursor cursor = cursorParser.decode(cursorStr);

        // 2. 리포지토리 조회 (size+1)
        List<NaverArticleEntity> articles = naverArticleRepository.findNewsByCondition(category, sort, size, cursor, keyword);

        // 3. hasNext / content
        boolean hasNext = articles.size() > size;
        List<NaverArticleEntity> content = hasNext ? articles.subList(0, size) : articles;

        // 4. 연관 용어 조회 (IN절) 및 Grouping
        List<Long> articleIds = content.stream().map(NaverArticleEntity::getId).toList();
        List<AiTermCardEntity> terms = aiTermCardRepository.findByArticleIdIn(articleIds);
        Map<Long, List<AiTermCardEntity>> articleTermMap = terms.stream()
                .collect(Collectors.groupingBy(card -> card.getArticle().getId()));

        // 5. nextCursor 생성 (sort 별로 분기)
        String nextCursor = null;
        if (hasNext && !content.isEmpty()) {
            NaverArticleEntity last = content.get(content.size() - 1);

            // 정렬 기준에 따라 필요한 정보만 커서에 담음
            if (sort == SortType.LATEST) {
                // 최신순일 때는 publishedAt과 id가 중요
                nextCursor = cursorParser.encode(last.getPublishedAt(), null, last.getId());
            } else if (sort == SortType.POPULARITY) {
                nextCursor = cursorParser.encode(null, last.getViewCount(), last.getId());
            }
        }

        return learningConverter.toNewListResponse(content, articleTermMap, category, sort, size, hasNext, nextCursor);
    }

    // 상세 뉴스 반환
    public LearningResponseDTO.NewsDetailResponse getNewsDetails(Long newsId) {
        NaverArticleEntity article = naverArticleRepository.findById(newsId)
                .orElseThrow(() -> new NaverArticleException(NaverArticleErrorCode.NAVER_ARTICLE_NOT_FOUND));

        AiArticleInsightEntity insight = aiArticleInsightRepository.findByArticleId(newsId)
                .orElseThrow(() -> new NaverArticleException(NaverArticleErrorCode.NAVER_ARTICLE_INSIGHT_NOT_FOUND));

        AiArticleSummaryEntity summary = aiArticleSummaryRepository.findByArticleId(newsId)
                .orElseThrow(() -> new NaverArticleException(NaverArticleErrorCode.NAVER_ARTICLE_SUMMERY_NOT_FOUND));

        List<AiTermCardEntity> terms = aiTermCardRepository.findByArticleIdOrderByCardOrderAsc(newsId);

        return learningConverter.toNewsDetailResponse(article, terms, summary, insight);
    }
}
