package com.finsight.finsight.domain.notification.infrastructure.template;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class NotificationTemplateBuilder {

    private final String siteUrl;

    public NotificationTemplateBuilder(@org.springframework.beans.factory.annotation.Value("${notification.site-url}") String siteUrl) {
        this.siteUrl = siteUrl;
    }

    /**
     * 일일 알림 HTML 이메일 생성
     */
    public String buildDailyEmail(boolean isNewsSaved, boolean isQuizSolved, boolean isQuizReviewed) {
        // 멘트 분기
        String message;
        String emoji;
        if (!isNewsSaved && !isQuizSolved && !isQuizReviewed) {
            message = "어제는 기록된 학습이 없었어요.<br>오늘은 뉴스 1개 저장하고, 퀴즈 한 번만 풀어 볼까요?";
            emoji = "💪";
        } else if (isNewsSaved && isQuizSolved && isQuizReviewed) {
            message = "어제 뉴스와 퀴즈 모두 잘 챙기셨어요.<br>오늘도 가볍게 뉴스 1개부터 이어가 볼까요?";
            emoji = "🎉";
        } else {
            message = "어제 저장한 뉴스가 아직 퀴즈를 기다리고 있어요.<br>오늘은 퀴즈 한 번만 이어서 풀어 볼까요?";
            emoji = "💪";
        }

        // 체크리스트 상태
        String newsStatus = buildStatusBadge("뉴스 저장", isNewsSaved);
        String quizStatus = buildStatusBadge("퀴즈 풀기", isQuizSolved);
        String reviewStatus = buildStatusBadge("복습하기", isQuizReviewed);

        // 날짜 포맷
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN));

        return buildDailyTemplate(dateStr, message, emoji, newsStatus, quizStatus, reviewStatus);
    }

    /**
     * 주간 알림 HTML 이메일 생성
     */
    public String buildWeeklyEmail(long quizCount, long newsCount) {
        // 멘트 분기
        String message;
        String emoji;
        if (quizCount == 0 && newsCount == 0) {
            message = "지난주에는 기록된 학습이 없었어요.<br>이번 주엔 뉴스 1개 저장부터 시작해 볼까요?";
            emoji = "🌱";
        } else {
            message = String.format("지난주에 퀴즈 세트 %d개, 뉴스 %d개를 공부했어요.<br>이번 주도 뉴스 1개부터 가볍게 시작해 볼까요?", quizCount, newsCount);
            emoji = "🚀";
        }

        // 날짜 범위 포맷
        LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = lastMonday.plusDays(6);
        String dateRangeStr = String.format("%s ~ %s",
                lastMonday.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")),
                lastSunday.format(DateTimeFormatter.ofPattern("M월 d일")));

        return buildWeeklyTemplate(dateRangeStr, message, emoji, quizCount, newsCount);
    }

    /**
     * 상태 뱃지 HTML 생성
     */
    private String buildStatusBadge(String label, boolean completed) {
        if (completed) {
            return String.format(
                    "<span style=\"display: inline-block; background-color: #ECFDF5; color: #059669; padding: 8px 14px; border-radius: 24px; font-size: 13px; font-weight: 500;\">✅ %s</span>",
                    label);
        } else {
            return String.format(
                    "<span style=\"display: inline-block; background-color: #FEF2F2; color: #DC2626; padding: 8px 14px; border-radius: 24px; font-size: 13px; font-weight: 500;\">❌ %s</span>",
                    label);
        }
    }

    /**
     * 일일 알림 HTML 템플릿
     */
    private String buildDailyTemplate(String dateStr, String message, String emoji,
                                       String newsStatus, String quizStatus, String reviewStatus) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 20px; background-color: #f0f0f0; font-family: -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.08);">
                    
                    <!-- 헤더 -->
                    <div style="background: linear-gradient(135deg, #818CF8 0%%, #6366F1 100%%); padding: 36px 24px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 26px; font-weight: 600;">📈 FinSight</h1>
                        <p style="color: rgba(255,255,255,0.95); margin: 10px 0 0 0; font-size: 14px; font-weight: 600;">금융 뉴스 학습 플랫폼</p>
                    </div>
                    
                    <!-- 본문 -->
                    <div style="padding: 36px 28px;">
                        <h2 style="color: #1F2937; margin: 0 0 6px 0; font-size: 19px; font-weight: 600;">🌅 오늘의 학습 알림</h2>
                        <p style="color: #9CA3AF; margin: 0 0 28px 0; font-size: 13px; font-weight: 400;">%s</p>
                        
                        <!-- 메시지 박스 -->
                        <div style="background-color: #F8FAFC; border-radius: 16px; padding: 24px; margin-bottom: 28px; border: 1px solid #F1F5F9;">
                            <p style="color: #374151; font-size: 15px; line-height: 1.8; margin: 0; font-weight: 600;">
                                %s %s
                            </p>
                        </div>
                        
                        <!-- 체크리스트 -->
                        <div style="margin-bottom: 28px;">
                            <p style="color: #94A3B8; font-size: 12px; margin: 0 0 14px 0; font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px;">어제 학습 현황</p>
                            <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                                <tr>
                                    <td style="padding-right: 8px; padding-bottom: 8px;">%s</td>
                                    <td style="padding-right: 8px; padding-bottom: 8px;">%s</td>
                                    <td style="padding-bottom: 8px;">%s</td>
                                </tr>
                            </table>
                        </div>
                        
                        <!-- CTA 버튼 -->
                        <a href="%s" style="display: block; width: 100%%; padding: 18px 24px; background: linear-gradient(135deg, #818CF8 0%%, #6366F1 100%%); color: white; text-decoration: none; border-radius: 14px; font-size: 15px; font-weight: 600; text-align: center; box-sizing: border-box; box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);">
                            지금 학습하러 가기 →
                        </a>
                    </div>
                    
                    <!-- 푸터 -->
                    <div style="padding: 24px 28px; background-color: #FAFAFA; text-align: center; border-top: 1px solid #F1F5F9;">
                        <p style="color: #B0B8C4; font-size: 11px; margin: 0 0 6px 0; font-weight: 400; line-height: 1.6;">이 메일은 FinSight 알림 설정에 의해 발송되었습니다.</p>
                        <p style="color: #B0B8C4; font-size: 11px; margin: 0; font-weight: 400;">
                            <a href="#" style="color: #9CA3AF; text-decoration: none; border-bottom: 1px solid #D1D5DB;">알림 설정 변경</a>
                            <span style="margin: 0 8px;">·</span>
                            <a href="#" style="color: #9CA3AF; text-decoration: none; border-bottom: 1px solid #D1D5DB;">수신 거부</a>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(dateStr, message, emoji, newsStatus, quizStatus, reviewStatus, siteUrl);
    }

    /**
     * 주간 알림 HTML 템플릿
     */
    private String buildWeeklyTemplate(String dateRangeStr, String message, String emoji, long quizCount, long newsCount) {
        // 통계 카드 스타일 (0이면 회색, 있으면 색상)
        String quizCardStyle = quizCount > 0
                ? "background: linear-gradient(145deg, #EEF2FF 0%, #E0E7FF 100%);"
                : "background: linear-gradient(145deg, #F3F4F6 0%, #E5E7EB 100%);";
        String quizTextColor = quizCount > 0 ? "#6366F1" : "#9CA3AF";
        String quizSubColor = quizCount > 0 ? "#818CF8" : "#9CA3AF";

        String newsCardStyle = newsCount > 0
                ? "background: linear-gradient(145deg, #ECFDF5 0%, #D1FAE5 100%);"
                : "background: linear-gradient(145deg, #F3F4F6 0%, #E5E7EB 100%);";
        String newsTextColor = newsCount > 0 ? "#10B981" : "#9CA3AF";
        String newsSubColor = newsCount > 0 ? "#34D399" : "#9CA3AF";

        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 20px; background-color: #f0f0f0; font-family: -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 8px 30px rgba(0,0,0,0.08);">
                    
                    <!-- 헤더 -->
                    <div style="background: linear-gradient(135deg, #34D399 0%%, #10B981 100%%); padding: 36px 24px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 26px; font-weight: 600;">📈 FinSight</h1>
                        <p style="color: rgba(255,255,255,0.95); margin: 10px 0 0 0; font-size: 14px; font-weight: 600;">금융 뉴스 학습 플랫폼</p>
                    </div>
                    
                    <!-- 본문 -->
                    <div style="padding: 36px 28px;">
                        <h2 style="color: #1F2937; margin: 0 0 6px 0; font-size: 19px; font-weight: 600;">📊 주간 학습 리포트</h2>
                        <p style="color: #9CA3AF; margin: 0 0 28px 0; font-size: 13px; font-weight: 400;">%s</p>
                        
                        <!-- 통계 카드 -->
                        <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="margin-bottom: 28px;">
                            <tr>
                                <td width="48%%" style="padding-right: 8px;">
                                    <div style="%s border-radius: 16px; padding: 24px; text-align: center;">
                                        <p style="color: %s; font-size: 36px; font-weight: 700; margin: 0;">%d</p>
                                        <p style="color: %s; font-size: 12px; margin: 6px 0 0 0; font-weight: 500;">퀴즈 세트</p>
                                    </div>
                                </td>
                                <td width="48%%" style="padding-left: 8px;">
                                    <div style="%s border-radius: 16px; padding: 24px; text-align: center;">
                                        <p style="color: %s; font-size: 36px; font-weight: 700; margin: 0;">%d</p>
                                        <p style="color: %s; font-size: 12px; margin: 6px 0 0 0; font-weight: 500;">뉴스 학습</p>
                                    </div>
                                </td>
                            </tr>
                        </table>
                        
                        <!-- 메시지 박스 -->
                        <div style="background-color: #F8FAFC; border-radius: 16px; padding: 24px; margin-bottom: 28px; border: 1px solid #F1F5F9;">
                            <p style="color: #374151; font-size: 15px; line-height: 1.8; margin: 0; font-weight: 600;">
                                %s %s
                            </p>
                        </div>
                        
                        <!-- CTA 버튼 -->
                        <a href="%s" style="display: block; width: 100%%; padding: 18px 24px; background: linear-gradient(135deg, #34D399 0%%, #10B981 100%%); color: white; text-decoration: none; border-radius: 14px; font-size: 15px; font-weight: 600; text-align: center; box-sizing: border-box; box-shadow: 0 4px 14px rgba(16, 185, 129, 0.35);">
                            이번 주 학습 시작하기 →
                        </a>
                    </div>
                    
                    <!-- 푸터 -->
                    <div style="padding: 24px 28px; background-color: #FAFAFA; text-align: center; border-top: 1px solid #F1F5F9;">
                        <p style="color: #B0B8C4; font-size: 11px; margin: 0 0 6px 0; font-weight: 400; line-height: 1.6;">이 메일은 FinSight 알림 설정에 의해 발송되었습니다.</p>
                        <p style="color: #B0B8C4; font-size: 11px; margin: 0; font-weight: 400;">
                            <a href="#" style="color: #9CA3AF; text-decoration: none; border-bottom: 1px solid #D1D5DB;">알림 설정 변경</a>
                            <span style="margin: 0 8px;">·</span>
                            <a href="#" style="color: #9CA3AF; text-decoration: none; border-bottom: 1px solid #D1D5DB;">수신 거부</a>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(dateRangeStr, quizCardStyle, quizTextColor, quizCount, quizSubColor,
                newsCardStyle, newsTextColor, newsCount, newsSubColor, message, emoji, siteUrl);
    }
}
