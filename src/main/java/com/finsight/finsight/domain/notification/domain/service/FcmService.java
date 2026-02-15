package com.finsight.finsight.domain.notification.domain.service;

import com.finsight.finsight.domain.auth.exception.AuthException;
import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.domain.notification.domain.constant.DeviceType;
import com.finsight.finsight.domain.notification.exception.NotificationException;
import com.finsight.finsight.domain.notification.exception.code.NotificationErrorCode;
import com.finsight.finsight.domain.notification.persistence.entity.FcmTokenEntity;
import com.finsight.finsight.domain.notification.persistence.repository.FcmTokenRepository;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * FCM 토큰 저장/업데이트
     */
    @Transactional
    public void saveToken(Long userId, String fcmToken, DeviceType deviceType) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 1. 이미 존재하는 토큰인지 확인
        Optional<FcmTokenEntity> existing = fcmTokenRepository.findByFcmToken(fcmToken);
        if (existing.isPresent()) {
            // 같은 유저면 무시
            if (existing.get().getUser().getUserId().equals(userId)) {
                return;
            }
            // 다른 유저 토큰이면 삭제 (기기 이전)
            fcmTokenRepository.delete(existing.get());
            fcmTokenRepository.flush();
        }

        // 2. 같은 유저 + 같은 deviceType이면 기존 토큰 삭제 (토큰 갱신)
        if (deviceType != null) {
            fcmTokenRepository.deleteByUserUserIdAndDeviceType(userId, deviceType);
        }

        // 3. 새 토큰 저장
        saveNewToken(user, fcmToken, deviceType);
    }

    private void saveNewToken(UserEntity user, String fcmToken, DeviceType deviceType) {
        FcmTokenEntity token = FcmTokenEntity.builder()
                .user(user)
                .fcmToken(fcmToken)
                .deviceType(deviceType)
                .build();
        fcmTokenRepository.save(token);
    }

    /**
     * 본인 토큰만 삭제
     */
    @Transactional
    public void deleteToken(Long userId, String fcmToken) {
        FcmTokenEntity token = fcmTokenRepository.findByFcmToken(fcmToken)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.FCM_TOKEN_NOT_FOUND));

        // 본인 토큰인지 확인
        if (!token.getUser().getUserId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.FCM_TOKEN_NOT_OWNED);
        }

        fcmTokenRepository.delete(token);
    }

    /**
     * 유저의 모든 토큰 삭제
     */
    @Transactional
    public void deleteAllTokens(Long userId) {
        fcmTokenRepository.deleteByUserUserId(userId);
    }

    /**
     * 단일 푸시 알림 발송
     */
    public void sendPushNotification(String fcmToken, String title, String body) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM 발송 성공: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 발송 실패 (token: {}): {}", fcmToken, e.getMessage());
            if (isInvalidTokenError(e)) {
                fcmTokenRepository.deleteByFcmToken(fcmToken);
                log.info("유효하지 않은 토큰 삭제: {}", fcmToken);
            }
        }
    }

    /**
     * 일일 알림 발송
     */
    public void sendDailyNotification(String fcmToken, boolean isNewsSaved, boolean isQuizSolved, boolean isQuizReviewed) {
        String title = "📚 오늘의 학습 알림";
        String body = buildDailyBody(isNewsSaved, isQuizSolved, isQuizReviewed);
        sendPushNotification(fcmToken, title, body);
    }

    /**
     * 주간 알림 발송
     */
    public void sendWeeklyNotification(String fcmToken, long quizCount, long newsCount) {
        String title = "📊 주간 학습 리포트";
        String body = buildWeeklyBody(quizCount, newsCount);
        sendPushNotification(fcmToken, title, body);
    }

    /**
     * 일일 알림 메시지 생성
     */
    private String buildDailyBody(boolean isNewsSaved, boolean isQuizSolved, boolean isQuizReviewed) {
        int completedCount = (isNewsSaved ? 1 : 0) + (isQuizSolved ? 1 : 0) + (isQuizReviewed ? 1 : 0);

        // 3개 모두 완료
        if (completedCount == 3) {
            return "어제 뉴스와 퀴즈 모두 잘 챙기셨어요. 오늘도 가볍게 뉴스 1개부터 이어가 볼까요?";
        }

        // 아무것도 안 함
        if (completedCount == 0) {
            return "어제는 기록된 학습이 없었어요. 오늘은 뉴스 1개 저장하고, 퀴즈 한 번만 풀어 볼까요?";
        }

        // 뉴스 저장만 한 경우
        if (isNewsSaved && !isQuizSolved) {
            return "어제 저장한 뉴스가 아직 퀴즈를 기다리고 있어요. 오늘은 퀴즈 한 번만 이어서 풀어 볼까요?";
        }

        // 그 외 (일부만 완료)
        return "어제 학습을 시작하셨네요! 오늘도 이어서 공부해 볼까요?";
    }

    /**
     * 주간 알림 메시지 생성
     */
    private String buildWeeklyBody(long quizCount, long newsCount) {
        // 학습이 전혀 없는 주
        if (quizCount == 0 && newsCount == 0) {
            return "지난주에는 기록된 학습이 없었어요. 이번 주엔 뉴스 1개 저장부터 시작해 볼까요?";
        }

        // 학습이 있었던 주
        return String.format(
                "지난주에 퀴즈 %d개, 뉴스 %d개를 공부했어요. 이번 주도 뉴스 1개부터 가볍게 시작해 볼까요?",
                quizCount, newsCount
        );
    }

    private boolean isInvalidTokenError(FirebaseMessagingException e) {
        String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        return errorCode.contains("UNREGISTERED") || errorCode.contains("INVALID");
    }
}