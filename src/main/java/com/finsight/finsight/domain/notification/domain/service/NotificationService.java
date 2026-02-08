package com.finsight.finsight.domain.notification.domain.service;

import com.finsight.finsight.domain.auth.domain.service.EmailService;
import com.finsight.finsight.domain.naver.persistence.repository.UserArticleViewRepository;
import com.finsight.finsight.domain.notification.infrastructure.template.NotificationTemplateBuilder;
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

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final FolderItemRepository folderItemRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserArticleViewRepository userArticleViewRepository;
    private final EmailService emailService;
    private final NotificationTemplateBuilder templateBuilder;

    /**
     * 일일 알림 발송 (매일 오전 8시)
     * 어제 학습 현황 기준
     */
    @Transactional(readOnly = true)
    public void sendDailyNotifications() {
        List<UserEntity> users = userRepository.findByNotificationEnabledAndAuthType(AuthType.EMAIL);

        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().atStartOfDay();

        for (UserEntity user : users) {
            try {
                String email = getEmailFromUser(user);
                if (email == null) continue;

                boolean isNewsSaved = folderItemRepository.existsByUserIdAndItemTypeAndSavedAtBetween(
                        user.getUserId(), FolderType.NEWS, yesterdayStart, yesterdayEnd);
                boolean isQuizSolved = quizAttemptRepository.existsNewAttemptBetween(
                        user.getUserId(), yesterdayStart, yesterdayEnd);
                boolean isQuizReviewed = quizAttemptRepository.existsReviewAttemptBetween(
                        user.getUserId(), yesterdayStart, yesterdayEnd);

                String htmlContent = templateBuilder.buildDailyEmail(isNewsSaved, isQuizSolved, isQuizReviewed);
                emailService.sendHtmlEmail(email, "[FinSight] 오늘의 학습 알림", htmlContent);
            } catch (Exception e) {
                // 개별 유저 실패해도 다음 유저 진행
            }
        }
    }

    /**
     * 주간 알림 발송 (매주 월요일 오전 9시)
     * 지난주 월~일 학습 현황 기준
     */
    @Transactional(readOnly = true)
    public void sendWeeklyNotifications() {
        List<UserEntity> users = userRepository.findByNotificationEnabledAndAuthType(AuthType.EMAIL);

        LocalDateTime lastWeekStart = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime lastWeekEnd = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();

        for (UserEntity user : users) {
            try {
                String email = getEmailFromUser(user);
                if (email == null) continue;

                Long quizCount = quizAttemptRepository.countByUserIdAndCreatedAtBetween(
                        user.getUserId(), lastWeekStart, lastWeekEnd);
                Long newsCount = userArticleViewRepository.countByUserIdAndViewedAtBetween(
                        user.getUserId(), lastWeekStart, lastWeekEnd);

                String htmlContent = templateBuilder.buildWeeklyEmail(
                        quizCount != null ? quizCount : 0,
                        newsCount != null ? newsCount : 0);
                emailService.sendHtmlEmail(email, "[FinSight] 주간 학습 리포트", htmlContent);
            } catch (Exception e) {
                // 개별 유저 실패해도 다음 유저 진행
            }
        }
    }

    private String getEmailFromUser(UserEntity user) {
        return user.getUserAuths().stream()
                .filter(auth -> auth.getAuthType() == AuthType.EMAIL)
                .map(UserAuthEntity::getIdentifier)
                .findFirst()
                .orElse(null);
    }
}
