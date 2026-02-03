package com.finsight.finsight.domain.notification.domain.service;

import com.finsight.finsight.domain.auth.domain.service.EmailService;
import com.finsight.finsight.domain.naver.persistence.repository.UserArticleViewRepository;
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

    /**
     * 일일 알림 발송 (매일 오전 8시)
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

                String message = buildDailyMessage(user.getUserId(), yesterdayStart, yesterdayEnd);
                emailService.sendNotificationEmail(email, "[FinSight] 오늘의 학습 알림", message);
            } catch (Exception e) {
                // 개별 유저 실패해도 다음 유저 진행
            }
        }
    }

    /**
     * 주간 알림 발송 (매주 월요일 오전 9시)
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

                String message = buildWeeklyMessage(user.getUserId(), lastWeekStart, lastWeekEnd);
                emailService.sendNotificationEmail(email, "[FinSight] 주간 학습 리포트", message);
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

    private String buildDailyMessage(Long userId, LocalDateTime start, LocalDateTime end) {
        boolean isNewsSaved = folderItemRepository.existsByUserIdAndItemTypeAndSavedAtBetween(
                userId, FolderType.NEWS, start, end);
        boolean isQuizSolved = quizAttemptRepository.existsNewAttemptBetween(userId, start, end);
        boolean isQuizReviewed = quizAttemptRepository.existsReviewAttemptBetween(userId, start, end);

        if (!isNewsSaved && !isQuizSolved && !isQuizReviewed) {
            return "어제는 기록된 학습이 없었어요. 오늘은 뉴스 1개 저장하고, 퀴즈 한 번만 풀어 볼까요?";
        } else if (isNewsSaved && isQuizSolved && isQuizReviewed) {
            return "어제 뉴스와 퀴즈 모두 잘 챙기셨어요. 오늘도 가볍게 뉴스 1개부터 이어가 볼까요?";
        } else {
            return "어제 저장한 뉴스가 아직 퀴즈를 기다리고 있어요. 오늘은 퀴즈 한 번만 이어서 풀어 볼까요?";
        }
    }

    private String buildWeeklyMessage(Long userId, LocalDateTime start, LocalDateTime end) {
        Long quizCount = quizAttemptRepository.countByUserIdAndCreatedAtBetween(userId, start, end);
        Long newsCount = userArticleViewRepository.countByUserIdAndViewedAtBetween(userId, start, end);

        if ((quizCount == null || quizCount == 0) && (newsCount == null || newsCount == 0)) {
            return "지난주에는 기록된 학습이 없었어요. 이번 주엔 뉴스 1개 저장부터 시작해 볼까요?";
        } else {
            return String.format("지난주에 퀴즈 세트 %d개, 뉴스 %d개를 공부했어요. 이번 주도 뉴스 1개부터 가볍게 시작해 볼까요?",
                    quizCount != null ? quizCount : 0,
                    newsCount != null ? newsCount : 0);
        }
    }
}
