package com.finsight.finsight.domain.mypage.application.dto.response;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import lombok.Builder;
import java.util.List;

public class LearningReportResponse {

    /**
     * 학습 리포트 응답
     */
    @Builder
    public record Report(
            // 주차 정보
            String weekStart,
            String weekEnd,
            int weeksAgo,

            // 누적 출석 일수
            long attendanceDays,

            // 전체 누적 통계
            long totalNewsSaved,
            long totalQuizSolved,
            long totalQuizReviewed,

            // 주간 요약 (상단 표시용)
            WeeklySummary weeklySummary,

            // 주간 비교 (막대 그래프용)
            WeeklyComparison weeklyComparison,

            // 카테고리 밸런스
            List<CategoryBalance> categoryBalance,

            // 일일 체크리스트 달성
            List<ChecklistStatus> checklistStatus) {
    }

    /**
     * 주간 요약 통계
     */
    @Builder
    public record WeeklySummary(
            int newsSaved,
            int quizSolved,
            int quizReviewed) {
    }

    /**
     * 주간 비교 (현재 주 vs 이전 주)
     */
    @Builder
    public record WeeklyComparison(
            WeeklyStats currentWeek,
            WeeklyStats previousWeek,
            WeeklyChange change) {
    }

    /**
     * 주차별 통계
     */
    @Builder
    public record WeeklyStats(
            int newsSaved,
            int quizSolved,
            int quizReviewed) {
    }

    /**
     * 주차 간 변화량
     */
    @Builder
    public record WeeklyChange(
            int newsSavedChange,
            int quizSolvedChange,
            int quizReviewedChange) {
    }

    /**
     * 카테고리 밸런스
     */
    @Builder
    public record CategoryBalance(
            String categoryName,
            NaverEconomySection section,
            long count,
            double percentage) {
    }

    /**
     * 체크리스트 달성 현황
     */
    @Builder
    public record ChecklistStatus(
            String dayOfWeek,
            int completionCount) {
    }
}