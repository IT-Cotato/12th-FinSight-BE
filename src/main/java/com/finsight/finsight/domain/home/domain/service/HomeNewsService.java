package com.finsight.finsight.domain.home.domain.service;

import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;
import com.finsight.finsight.domain.ai.persistence.repository.AiTermCardRepository;
import com.finsight.finsight.domain.category.persistence.entity.UserCategoryEntity;
import com.finsight.finsight.domain.category.persistence.repository.UserCategoryRepository;
import com.finsight.finsight.domain.home.application.dto.response.HomeResponseDTO;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.domain.quiz.persistence.repository.QuizAttemptRepository;
import com.finsight.finsight.domain.storage.persistence.entity.FolderItemEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderItemRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class HomeNewsService {

    private final NaverArticleRepository naverArticleRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final AiTermCardRepository aiTermCardRepository;
    private final FolderItemRepository folderItemRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    private static final int CATEGORY_COUNT = NaverEconomySection.values().length; // 8
    private static final int PERSONALIZED_NEWS_SIZE = 8;

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

    /**
     * 맞춤 뉴스 조회
     * - 종합(category=null): 관심사 카테고리 우선 + 나머지 카테고리 라운드 로빈으로 8개
     * - 특정 카테고리: 해당 카테고리 최신순 8개
     */
    public HomeResponseDTO.PersonalizedNewsResponse getPersonalizedNews(Long userId, NaverEconomySection category) {
        List<NaverArticleEntity> articles;

        if (category != null) {
            // 특정 카테고리: 최신순 8개
            articles = naverArticleRepository.findTopLatestBySection(category, PERSONALIZED_NEWS_SIZE);
        } else {
            // 종합: 관심사 우선 + 나머지 라운드 로빈
            articles = getPersonalizedNewsForAll(userId);
        }

        // 기사 ID 목록으로 용어 카드 한번에 조회 (N+1 방지)
        List<Long> articleIds = articles.stream().map(NaverArticleEntity::getId).toList();
        Map<Long, List<HomeResponseDTO.TermItem>> termsByArticleId = getTermsByArticleIds(articleIds);

        List<HomeResponseDTO.PersonalizedNewsItem> newsItems = articles.stream()
                .map(article -> HomeResponseDTO.PersonalizedNewsItem.builder()
                        .newsId(article.getId())
                        .category(article.getSection())
                        .terms(termsByArticleId.getOrDefault(article.getId(), List.of()))
                        .title(article.getTitle())
                        .thumbnailUrl(article.getThumbnailUrl())
                        .build())
                .toList();

        return HomeResponseDTO.PersonalizedNewsResponse.builder()
                .category(category)
                .news(newsItems)
                .build();
    }

    /**
     * 여러 기사에 대한 용어 카드 조회 (기사별 최대 3개)
     */
    private Map<Long, List<HomeResponseDTO.TermItem>> getTermsByArticleIds(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        List<AiTermCardEntity> termCards = aiTermCardRepository.findByArticleIdIn(articleIds);

        return termCards.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getArticle().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                cards -> cards.stream()
                                        .sorted(Comparator.comparingInt(AiTermCardEntity::getCardOrder))
                                        .limit(3)
                                        .map(tc -> HomeResponseDTO.TermItem.builder()
                                                .termId(tc.getTerm().getId())
                                                .displayName(tc.getTerm().getDisplayName())
                                                .build())
                                        .toList()
                        )
                ));
    }

    /**
     * 종합 맞춤 뉴스 조회 로직
     * 1. 사용자 관심사 카테고리에서 라운드 로빈 (우선)
     * 2. 나머지 카테고리에서 라운드 로빈으로 8개 채울 때까지
     * 3. 아직 부족하면 관심사 카테고리에서 추가로 채움
     */
    private List<NaverArticleEntity> getPersonalizedNewsForAll(Long userId) {
        // 사용자 관심사 카테고리 조회
        List<NaverEconomySection> interestSections = userCategoryRepository.findByUserUserId(userId)
                .stream()
                .map(UserCategoryEntity::getSection)
                .toList();

        Set<Long> usedArticleIds = new HashSet<>();
        List<NaverArticleEntity> result = new ArrayList<>();

        // 관심사 카테고리별 기사 미리 조회 (충분히 많이)
        Map<NaverEconomySection, List<NaverArticleEntity>> interestArticles = new EnumMap<>(NaverEconomySection.class);
        for (NaverEconomySection section : interestSections) {
            interestArticles.put(section, naverArticleRepository.findTopLatestBySection(section, PERSONALIZED_NEWS_SIZE));
        }

        // 1. 관심사 카테고리에서 라운드 로빈으로 1개씩
        int round = 0;
        int maxInterestRounds = interestArticles.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        while (result.size() < PERSONALIZED_NEWS_SIZE && round < maxInterestRounds) {
            for (NaverEconomySection section : interestSections) {
                if (result.size() >= PERSONALIZED_NEWS_SIZE) break;

                List<NaverArticleEntity> articles = interestArticles.get(section);
                if (articles != null && round < articles.size()) {
                    NaverArticleEntity article = articles.get(round);
                    if (!usedArticleIds.contains(article.getId())) {
                        result.add(article);
                        usedArticleIds.add(article.getId());
                    }
                }
            }
            // 관심사에서 각 1개씩만 우선 배치 후 나머지 카테고리로 넘어감
            if (round == 0) break;
            round++;
        }

        // 2. 나머지 카테고리에서 라운드 로빈으로 채움
        if (result.size() < PERSONALIZED_NEWS_SIZE) {
            Set<NaverEconomySection> interestSet = new HashSet<>(interestSections);
            List<NaverEconomySection> remainingSections = Arrays.stream(NaverEconomySection.values())
                    .filter(s -> !interestSet.contains(s))
                    .toList();

            Map<NaverEconomySection, List<NaverArticleEntity>> remainingArticles = new EnumMap<>(NaverEconomySection.class);
            for (NaverEconomySection section : remainingSections) {
                remainingArticles.put(section, naverArticleRepository.findTopLatestBySection(section, PERSONALIZED_NEWS_SIZE));
            }

            round = 0;
            int maxRemainingRounds = remainingArticles.values().stream()
                    .mapToInt(List::size)
                    .max()
                    .orElse(0);

            while (result.size() < PERSONALIZED_NEWS_SIZE && round < maxRemainingRounds) {
                for (NaverEconomySection section : remainingSections) {
                    if (result.size() >= PERSONALIZED_NEWS_SIZE) break;

                    List<NaverArticleEntity> articles = remainingArticles.get(section);
                    if (articles != null && round < articles.size()) {
                        NaverArticleEntity article = articles.get(round);
                        if (!usedArticleIds.contains(article.getId())) {
                            result.add(article);
                            usedArticleIds.add(article.getId());
                        }
                    }
                }
                round++;
            }
        }

        // 3. 아직 부족하면 관심사 카테고리에서 추가로 채움
        if (result.size() < PERSONALIZED_NEWS_SIZE) {
            round = 1; // 이미 0라운드는 사용함
            while (result.size() < PERSONALIZED_NEWS_SIZE && round < maxInterestRounds) {
                for (NaverEconomySection section : interestSections) {
                    if (result.size() >= PERSONALIZED_NEWS_SIZE) break;

                    List<NaverArticleEntity> articles = interestArticles.get(section);
                    if (articles != null && round < articles.size()) {
                        NaverArticleEntity article = articles.get(round);
                        if (!usedArticleIds.contains(article.getId())) {
                            result.add(article);
                            usedArticleIds.add(article.getId());
                        }
                    }
                }
                round++;
            }
        }

        return result;
    }

    /**
     * 홈 상태 메시지 조회
     */
    public HomeResponseDTO.HomeStatusResponse getHomeStatus(Long userId) {
        // 1. 최근 보관한 뉴스 최대 5개 조회 (FolderItemRepository 활용)
        List<FolderItemEntity> recentSavedItems = folderItemRepository.findByUserIdAndItemType(
                userId, FolderType.NEWS, PageRequest.of(0, 5)
        ).getContent();

        int a = recentSavedItems.size();
        if (a == 0) {
            return HomeResponseDTO.HomeStatusResponse.builder()
                    .message("아직 보관한 뉴스가 없어요. 뉴스를 읽고 저장해보세요.")
                    .savedCount(0)
                    .unsolvedCount(0)
                    .build();
        }

        // 2. 보관된 기사 ID 추출
        List<Long> articleIds = recentSavedItems.stream()
                .map(FolderItemEntity::getItemId)
                .toList();

        // 3. 그중 풀이가 완료된 기사 ID 목록 조회 (N+1 방지)
        List<Long> solvedArticleIds = quizAttemptRepository.findSolvedArticleIds(userId, articleIds);

        // 4. 안 푼 뉴스 개수(b) 계산
        int b = a - solvedArticleIds.size();

        // 5. 메시지 구성
        String message;
        if (b > 0) {
            message = String.format("최근에 보관한 %d개 뉴스 중, 아직 퀴즈를 풀지 않은 뉴스 %d개를 풀어봐요.", a, b);
        } else {
            message = String.format("최근에 보관한 뉴스 %d개의 퀴즈를 모두 풀었어요. 새로운 뉴스를 저장해서 계속 넓혀가 볼까요?", a);
        }

        return HomeResponseDTO.HomeStatusResponse.builder()
                .message(message)
                .savedCount(a)
                .unsolvedCount(b)
                .build();
    }

    /**
     * 일일 퀘스트 조회
     */
    public HomeResponseDTO.DailyChecklistResponse getDailyChecklist(Long userId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // 1. 뉴스 1개 저장하기 여부
        // 조건: 오늘 savedAt이 기록된 NEWS 타입 폴더 아이템이 존재하는가?
        // (재저장/카테고리 변경 시 savedAt이 업데이트된다고 가정)
        boolean isNewsSaved = folderItemRepository.existsByUserIdAndItemTypeAndSavedAtAfter(
                userId, FolderType.NEWS, startOfToday);

        // 2. 퀴즈 1개 풀기 여부
        // 조건: 오늘 attemptedAt이 기록된 퀴즈 중, '처음' 푼 기록인가?
        // (QuizAttemptEntity의 createdAt과 attemptedAt이 오늘로 동일한 경우 = 신규 풀이)
        boolean isQuizSolved = quizAttemptRepository.existsNewAttemptToday(userId, startOfToday);

        // 3. 보관한 퀴즈 복습하기 여부
        // 조건: 오늘 attemptedAt이 기록된 퀴즈 중, 이미 과거에 풀었던 기록을 업데이트한 경우인가?
        // (QuizAttemptEntity의 updateAttempt 메서드가 호출되어 attemptedAt만 오늘인 경우)
        boolean isQuizReviewed = quizAttemptRepository.existsReviewAttemptToday(userId, startOfToday);

        return HomeResponseDTO.DailyChecklistResponse.builder()
                .isNewsSaved(isNewsSaved)
                .isQuizSolved(isQuizSolved)
                .isQuizReviewed(isQuizReviewed)
                .build();
    }
}
