package com.finsight.finsight.domain.home.domain.service;

import com.finsight.finsight.domain.home.application.dto.response.HomeResponseDTO;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class HomeNewsService {

    private final NaverArticleRepository naverArticleRepository;

    private static final int CATEGORY_COUNT = NaverEconomySection.values().length; // 8

    /**
     * 카테고리별 인기순 라운드 로빈 방식 조회
     * (금융 1위 → 증권 1위 → ... → 경제일반 1위 → 금융 2위 → 증권 2위 → ...)
     * 캐시 TTL: 5분
     */
    @Cacheable(value = "popularNews", key = "#size + '_' + (#cursorStr ?: 'first')")
    public HomeResponseDTO.PopularNewsResponse getPopularNewsByCategory(int size, String cursorStr) {
        // 1. 커서 디코딩 (offset 값)
        int offset = decodeCursor(cursorStr);

        // 2. 필요한 기사 수 계산: (offset + size) / 8 + 여유분
        int perCategoryLimit = (offset + size) / CATEGORY_COUNT + 10;

        // 3. 각 카테고리별 인기순 기사 조회
        Map<NaverEconomySection, List<NaverArticleEntity>> articlesBySection = new EnumMap<>(NaverEconomySection.class);
        for (NaverEconomySection section : NaverEconomySection.values()) {
            List<NaverArticleEntity> articles = naverArticleRepository.findTopPopularBySection(section, perCategoryLimit);
            articlesBySection.put(section, articles);
        }

        // 4. 라운드 로빈 병합
        List<NaverArticleEntity> merged = mergeRoundRobin(articlesBySection);

        // 5. offset + size + 1로 슬라이싱 (hasNext 판별용)
        int endIndex = Math.min(offset + size + 1, merged.size());
        List<NaverArticleEntity> sliced = offset < merged.size()
                ? merged.subList(offset, endIndex)
                : List.of();

        // 6. hasNext / content 분리
        boolean hasNext = sliced.size() > size;
        List<NaverArticleEntity> content = hasNext ? sliced.subList(0, size) : sliced;

        // 7. DTO 변환
        List<HomeResponseDTO.PopularNewsItem> newsItems = content.stream()
                .map(article -> HomeResponseDTO.PopularNewsItem.builder()
                        .newsId(article.getId())
                        .category(article.getSection())
                        .title(article.getTitle())
                        .thumbnailUrl(article.getThumbnailUrl())
                        .build())
                .toList();

        // 8. nextCursor 생성
        String nextCursor = null;
        if (hasNext) {
            nextCursor = encodeCursor(offset + size);
        }

        return HomeResponseDTO.PopularNewsResponse.builder()
                .size(size)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .news(newsItems)
                .build();
    }

    /**
     * 라운드 로빈 병합: 각 카테고리에서 순서대로 하나씩 가져옴
     */
    private List<NaverArticleEntity> mergeRoundRobin(Map<NaverEconomySection, List<NaverArticleEntity>> articlesBySection) {
        List<NaverArticleEntity> result = new ArrayList<>();
        NaverEconomySection[] sections = NaverEconomySection.values();

        int maxSize = articlesBySection.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        for (int round = 0; round < maxSize; round++) {
            for (NaverEconomySection section : sections) {
                List<NaverArticleEntity> articles = articlesBySection.get(section);
                if (articles != null && round < articles.size()) {
                    result.add(articles.get(round));
                }
            }
        }

        return result;
    }

    private int decodeCursor(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) {
            return 0;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cursorStr);
            return Integer.parseInt(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return 0;
        }
    }

    private String encodeCursor(int offset) {
        return Base64.getEncoder().encodeToString(
                String.valueOf(offset).getBytes(StandardCharsets.UTF_8)
        );
    }
}
