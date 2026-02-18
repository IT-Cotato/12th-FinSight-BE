package com.finsight.finsight.domain.notification.domain.service;

import com.finsight.finsight.domain.auth.domain.service.EmailService;
import com.finsight.finsight.domain.naver.persistence.repository.UserArticleViewRepository;
import com.finsight.finsight.domain.notification.infrastructure.template.NotificationTemplateBuilder;
import com.finsight.finsight.domain.notification.persistence.entity.FcmTokenEntity;
import com.finsight.finsight.domain.notification.persistence.repository.FcmTokenRepository;
import com.finsight.finsight.domain.quiz.persistence.repository.QuizAttemptRepository;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderItemRepository;
import com.finsight.finsight.domain.user.domain.constant.AuthType;
import com.finsight.finsight.domain.user.persistence.entity.UserAuthEntity;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final FolderItemRepository folderItemRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserArticleViewRepository userArticleViewRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmService fcmService;


    private final EmailService emailService;
    private final NotificationTemplateBuilder templateBuilder;

    /**
     * 일일 알림 발송 (매일 오전 8시)
     * 어제 학습 현황 기준
     */
    @Transactional(readOnly = true)
    public void sendDailyNotifications() {
        List<UserEntity> users = userRepository.findByNotificationEnabledTrue();

        // 유저별 FCM 토큰 맵 생성 (N+1 방지)
        List<Long> userIds = users.stream().map(UserEntity::getUserId).toList();
        Map<Long, List<FcmTokenEntity>> tokenMap = getTokenMapByUserIds(userIds);

        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().atStartOfDay();

        Set<Long> newsUserIds = folderItemRepository.findUserIdsByItemTypeAndSavedAtBetween(
                FolderType.NEWS, yesterdayStart, yesterdayEnd);
        Set<Long> quizUserIds = quizAttemptRepository.findUserIdsWithNewAttemptBetween(
                yesterdayStart, yesterdayEnd);
        Set<Long> reviewUserIds = quizAttemptRepository.findUserIdsWithReviewAttemptBetween(
                yesterdayStart, yesterdayEnd);

        for (UserEntity user : users) {
            try {
                boolean isNewsSaved = newsUserIds.contains(user.getUserId());
                boolean isQuizSolved = quizUserIds.contains(user.getUserId());
                boolean isQuizReviewed = reviewUserIds.contains(user.getUserId());


                String email = getEmailFromUser(user);
                if (email != null) {
                    String htmlContent = templateBuilder.buildDailyEmail(isNewsSaved, isQuizSolved, isQuizReviewed);
                    emailService.sendHtmlEmail(email, "[FinSight] 오늘의 학습 알림", htmlContent);
                }

                // 유저의 모든 기기에 FCM 발송
                List<FcmTokenEntity> tokens = tokenMap.getOrDefault(user.getUserId(), List.of());
                for (FcmTokenEntity token : tokens) {
                    fcmService.sendDailyNotification(token.getFcmToken(), isNewsSaved, isQuizSolved, isQuizReviewed);
                }

            } catch (Exception e) {
                // 개별 유저 실패해도 다음 유저 진행, 추후 로그 추가
            }
        }
    }

    /**
     * 주간 알림 발송 (매주 월요일 오전 9시)
     * 지난주 월~일 학습 현황 기준
     */
    @Transactional(readOnly = true)
    public void sendWeeklyNotifications() {
        List<UserEntity> users = userRepository.findByNotificationEnabledTrue();

        // 유저별 FCM 토큰 맵 생성 (N+1 방지)
        List<Long> userIds = users.stream().map(UserEntity::getUserId).toList();
        Map<Long, List<FcmTokenEntity>> tokenMap = getTokenMapByUserIds(userIds);

        LocalDateTime lastWeekStart = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime lastWeekEnd = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();

        Map<Long, Long> quizCountMap = quizAttemptRepository.countByUserGroupedBetween(lastWeekStart, lastWeekEnd)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> newsCountMap = userArticleViewRepository.countByUserGroupedBetween(lastWeekStart, lastWeekEnd)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        for (UserEntity user : users) {
            try {
                long quizCount = quizCountMap.getOrDefault(user.getUserId(), 0L);
                long newsCount = newsCountMap.getOrDefault(user.getUserId(), 0L);

                String email = getEmailFromUser(user);
                if (email != null) {
                    String htmlContent = templateBuilder.buildWeeklyEmail(quizCount, newsCount);
                    emailService.sendHtmlEmail(email, "[FinSight] 주간 학습 리포트", htmlContent);
                }

                // 유저의 모든 기기에 FCM 발송
                List<FcmTokenEntity> tokens = tokenMap.getOrDefault(user.getUserId(), List.of());
                for (FcmTokenEntity token : tokens) {
                    fcmService.sendWeeklyNotification(token.getFcmToken(), quizCount, newsCount);
                }
            } catch (Exception e) {
                // 개별 유저 실패해도 다음 유저 진행, 추후 로그 추가
            }
        }
    }

    /**
     * 유저 ID 목록으로 토큰 맵 조회 (N+1 방지)
     */
    private Map<Long, List<FcmTokenEntity>> getTokenMapByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return fcmTokenRepository.findByUserUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(token -> token.getUser().getUserId()));
    }



    private String getEmailFromUser(UserEntity user) {
        return user.getUserAuths().stream()
                .filter(auth -> auth.getAuthType() == AuthType.EMAIL)
                .map(UserAuthEntity::getIdentifier)
                .findFirst()
                .orElse(null);
    }

}
