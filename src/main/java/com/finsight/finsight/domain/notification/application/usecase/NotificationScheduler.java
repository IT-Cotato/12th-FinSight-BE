package com.finsight.finsight.domain.notification.application.usecase;

import com.finsight.finsight.domain.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * 매일 오전 8시 일일 알림 발송
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyNotification() {
        notificationService.sendDailyNotifications();
    }

    /**
     * 매주 월요일 오전 9시 주간 알림 발송
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyNotification() {
        notificationService.sendWeeklyNotifications();
    }
}
