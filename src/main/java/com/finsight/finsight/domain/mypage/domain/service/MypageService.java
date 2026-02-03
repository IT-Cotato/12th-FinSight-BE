package com.finsight.finsight.domain.mypage.domain.service;

import com.finsight.finsight.domain.auth.exception.AuthException;
import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.domain.category.application.dto.request.SaveCategoryRequest;
import com.finsight.finsight.domain.category.domain.service.CategoryService;
import com.finsight.finsight.domain.category.persistence.repository.UserCategoryOrderRepository;
import com.finsight.finsight.domain.category.persistence.repository.UserCategoryRepository;
import com.finsight.finsight.domain.mypage.application.dto.request.UpdateNotificationRequest;
import com.finsight.finsight.domain.mypage.application.dto.request.UpdateProfileRequest;
import com.finsight.finsight.domain.mypage.application.dto.response.LearningReportResponse;
import com.finsight.finsight.domain.mypage.application.dto.response.MypageResponse;
import com.finsight.finsight.domain.mypage.application.dto.response.NotificationResponse;
import com.finsight.finsight.domain.mypage.exception.MypageException;
import com.finsight.finsight.domain.mypage.exception.code.MypageErrorCode;
import com.finsight.finsight.domain.mypage.persistence.mapper.MypageConverter;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.persistence.repository.UserArticleViewRepository;
import com.finsight.finsight.domain.quiz.persistence.repository.QuizAttemptRepository;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderItemRepository;
import com.finsight.finsight.domain.storage.persistence.repository.FolderRepository;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final CategoryService categoryService;
    private final UserArticleViewRepository userArticleViewRepository;
    private final FolderItemRepository folderItemRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final FolderRepository folderRepository;
    private final UserCategoryOrderRepository userCategoryOrderRepository;

    public MypageResponse.MemberProfileResponse getUserProfile(Long userId) {
        // db에서 조회에 실패한 경우
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        return MypageConverter.toMypageProfileResponse(user);
    }

    @Transactional
    public void withdrawMember(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new MypageException(MypageErrorCode.MEMBER_NOT_FOUND);
        }

        // 1. 퀴즈 풀이 기록 삭제
        quizAttemptRepository.deleteByUserUserId(userId);

        // 2. 보관함(폴더) 및 아이템 삭제
        folderRepository.deleteByUserUserId(userId);

        // 3. 관심 카테고리 설정 삭제
        userCategoryOrderRepository.deleteByUserUserId(userId);
        userCategoryRepository.deleteByUserUserId(userId);

        // 4. 기사 열람 기록 삭제
        userArticleViewRepository.deleteByUserUserId(userId);

        // 5. 사용자 삭제
        userRepository.deleteById(userId);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 변경
        if (!user.getNickname().equals(request.nickname())) {
            validateNicknameFormat(request.nickname());
            user.updateNickname(request.nickname());
        }

        // 카테고리 업데이트 (CategoryService 재사용)
        SaveCategoryRequest saveCategoryRequest = new SaveCategoryRequest(request.categories());
        categoryService.saveCategories(userId, saveCategoryRequest);
    }

    private void validateNicknameFormat(String nickname) {
        if (nickname == null || nickname.length() < 1 || nickname.length() > 10) {
            throw new AuthException(
                    AuthErrorCode.INVALID_NICKNAME_FORMAT);
        }
    }

    @Transactional(readOnly = true)
    public void checkNickname(Long userId, String nickname) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        // 본인 닉네임인 경우 중복 체크 패스
        if (user.getNickname().equals(nickname)) {
            return;
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new MypageException(MypageErrorCode.DUPLICATE_NICKNAME);
        }
    }

    /**
     * 학습 리포트 조회
     */
    @Transactional(readOnly = true)
    public LearningReportResponse.Report getLearningReport(
        Long userId,
        LocalDate weekStart,
        LocalDate weekEnd,
        int weeksAgo) {

        LocalDateTime currentStartDateTime = weekStart.atStartOfDay();
        LocalDateTime currentEndDateTime = weekEnd.atTime(23, 59, 59);

        LocalDate previousWeekStart = weekStart.minusWeeks(1);
        LocalDate previousWeekEnd = weekEnd.minusWeeks(1);
        LocalDateTime previousStartDateTime = previousWeekStart.atStartOfDay();
        LocalDateTime previousEndDateTime = previousWeekEnd.atTime(23, 59, 59);

        LearningReportResponse.WeeklyStats currentWeekStats = buildWeeklyStats(
            userId, currentStartDateTime, currentEndDateTime);

        LearningReportResponse.WeeklyStats previousWeekStats = buildWeeklyStats(
            userId, previousStartDateTime, previousEndDateTime);

        LearningReportResponse.WeeklyChange change = LearningReportResponse.WeeklyChange.builder()
            .newsSavedChange(currentWeekStats.newsSaved() - previousWeekStats.newsSaved())
            .quizSolvedChange(currentWeekStats.quizSolved() - previousWeekStats.quizSolved())
            .quizReviewedChange(currentWeekStats.quizReviewed() - previousWeekStats.quizReviewed())
            .build();

        LearningReportResponse.WeeklySummary weeklySummary = LearningReportResponse.WeeklySummary.builder()
            .newsSaved(currentWeekStats.newsSaved())
            .quizSolved(currentWeekStats.quizSolved())
            .quizReviewed(currentWeekStats.quizReviewed())
            .build();

        LearningReportResponse.WeeklyComparison weeklyComparison = LearningReportResponse.WeeklyComparison.builder()
            .currentWeek(currentWeekStats)
            .previousWeek(previousWeekStats)
            .change(change)
            .build();

        List<LearningReportResponse.CategoryBalance> categoryBalance = buildCategoryBalance(
            userId, currentStartDateTime, currentEndDateTime);

        List<LearningReportResponse.ChecklistStatus> checklistStatus = buildChecklistStatus(
            userId, currentStartDateTime, currentEndDateTime, weekStart);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        Long totalNewsSaved = folderItemRepository.countByUserIdAndItemType(userId, FolderType.NEWS);
        Long totalQuizSolved = quizAttemptRepository.countByUserUserId(userId);
        Long totalQuizReviewed = quizAttemptRepository.countReviewsByUserId(userId);
        Long attendanceDays = user.getAttendanceCount();

        return LearningReportResponse.Report.builder()
            .weekStart(weekStart.toString())
            .weekEnd(weekEnd.toString())
            .weeksAgo(weeksAgo)
            .attendanceDays(attendanceDays != null ? attendanceDays : 0L)
            .totalNewsSaved(totalNewsSaved != null ? totalNewsSaved : 0L)
            .totalQuizSolved(totalQuizSolved != null ? totalQuizSolved : 0L)
            .totalQuizReviewed(totalQuizReviewed != null ? totalQuizReviewed : 0L)
            .weeklySummary(weeklySummary)
            .weeklyComparison(weeklyComparison)
            .categoryBalance(categoryBalance)
            .checklistStatus(checklistStatus)
            .build();
    }

    private LearningReportResponse.WeeklyStats buildWeeklyStats(
        Long userId, LocalDateTime start, LocalDateTime end) {

        int newsSaved = folderItemRepository.findSavedItemsByWeek(userId, FolderType.NEWS, start, end).size();
        int quizSolved = quizAttemptRepository.findAttemptsByWeek(userId, start, end).size();
        int quizReviewed = quizAttemptRepository.findReviewAttemptsByWeek(userId, start, end).size();

        return LearningReportResponse.WeeklyStats.builder()
            .newsSaved(newsSaved)
            .quizSolved(quizSolved)
            .quizReviewed(quizReviewed)
            .build();
    }

    private List<LearningReportResponse.CategoryBalance> buildCategoryBalance(
        Long userId, LocalDateTime start, LocalDateTime end) {

        List<Object[]> rawData = userArticleViewRepository.findCategoryBalanceByWeek(userId, start, end);

        if (rawData == null || rawData.isEmpty()) {
            return Collections.emptyList();
        }

        long total = rawData.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();

        return rawData.stream()
            .map(row -> {
                String sectionStr = (String) row[0];
                NaverEconomySection section = NaverEconomySection.valueOf(sectionStr);
                long count = ((Number) row[1]).longValue();
                double percentage = total > 0 ? (count * 100.0 / total) : 0.0;

                return LearningReportResponse.CategoryBalance.builder()
                    .categoryName(section.name())
                    .section(section)
                    .count(count)
                    .percentage(Math.round(percentage * 10) / 10.0)
                    .build();
            }).toList();
    }

    private List<LearningReportResponse.ChecklistStatus> buildChecklistStatus(
        Long userId, LocalDateTime start, LocalDateTime end,
        LocalDate startOfWeek) {

        Map<LocalDate, Integer> newsMap = new HashMap<>();
        Map<LocalDate, Integer> quizMap = new HashMap<>();
        Map<LocalDate, Integer> reviewMap = new HashMap<>();

        folderItemRepository.findSavedItemsByWeek(userId, FolderType.NEWS, start, end).stream()
            .collect(Collectors.groupingBy(fi -> fi.getSavedAt().toLocalDate(), Collectors.counting()))
            .forEach((date, count) -> newsMap.put(date, count.intValue()));

        quizAttemptRepository.findAttemptsByWeek(userId, start, end).stream()
            .collect(Collectors.groupingBy(qa -> qa.getAttemptedAt().toLocalDate(), Collectors.counting()))
            .forEach((date, count) -> quizMap.put(date, count.intValue()));

        quizAttemptRepository.findReviewAttemptsByWeek(userId, start, end).stream()
            .collect(Collectors.groupingBy(qa -> qa.getAttemptedAt().toLocalDate(), Collectors.counting()))
            .forEach((date, count) -> reviewMap.put(date, count.intValue()));

        List<LearningReportResponse.ChecklistStatus> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            String dayOfWeek = getDayOfWeekKorean(date.getDayOfWeek());

            int completionCount = (newsMap.getOrDefault(date, 0) > 0 ? 1 : 0)
                + (quizMap.getOrDefault(date, 0) > 0 ? 1 : 0)
                + (reviewMap.getOrDefault(date, 0) > 0 ? 1 : 0);

            result.add(LearningReportResponse.ChecklistStatus.builder()
                .dayOfWeek(dayOfWeek)
                .completionCount(completionCount)
                .build());
        }

        return result;
    }

    private String getDayOfWeekKorean(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    /**
     * 알림 설정 조회
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationSetting(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        return NotificationResponse.builder()
                .enabled(user.getNotificationEnabled())
                .build();
    }

    /**
     * 알림 설정 변경
     */
    @Transactional
    public void updateNotificationSetting(Long userId, UpdateNotificationRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        user.updateNotificationEnabled(request.enabled());
    }
}