package com.finsight.finsight.domain.learning.domain.service;

import com.finsight.finsight.domain.learning.application.dto.response.LearningResponseDTO;
import com.finsight.finsight.domain.learning.domain.constant.Category;
import com.finsight.finsight.domain.learning.domain.constant.SortType;
import com.finsight.finsight.domain.learning.persistence.mapper.LearningConverter;
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

    public LearningResponseDTO.NewListResponse getNewsList(Category category, SortType sort, int size,
            String cursorStr) {
        // 1. 커서 디코딩
        CursorParser.NewsCursor cursor = cursorParser.decode(cursorStr);

        // 2. 리포지토리 조회 (size+1)
        List<NaverArticleEntity> articles = naverArticleRepository.findNewsByCondition(category, sort, size, cursor);

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
                // TODO: 추후 viewCount 컬럼 생성 시 last.getViewCount()로 교체
                nextCursor = cursorParser.encode(null, 0L, last.getId());
            }
        }

        return LearningConverter.toNewListResponse(content, articleTermMap, category, sort, size, hasNext, nextCursor);
    }
}
