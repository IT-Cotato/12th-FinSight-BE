package com.finsight.finsight.domain.notification.application.usecase;

import com.finsight.finsight.domain.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * 매일 오전 9시 일일 알림 발송
     */
    @Scheduled(cron = "${notification.scheduler.daily-cron}")
    public void sendDailyNotification() {
        notificationService.sendDailyNotifications();
    }

    /**
     * 매주 월요일 오전 9시 주간 알림 발송
     */
    @Scheduled(cron = "${notification.scheduler.weekly-cron}")
    public void sendWeeklyNotification() {
        notificationService.sendWeeklyNotifications();
    }
}
