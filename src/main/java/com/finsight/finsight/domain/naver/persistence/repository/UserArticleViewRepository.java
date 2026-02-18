package com.finsight.finsight.domain.naver.persistence.repository;

import com.finsight.finsight.domain.naver.persistence.entity.UserArticleViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserArticleViewRepository extends JpaRepository<UserArticleViewEntity, Long> {

    /**
     * 주차별 일별 학습량 조회 (읽은 뉴스 개수)
     */
    @Query(value = """
        SELECT TRUNC(viewed_at) as activity_date, COUNT(*) as count
        FROM user_article_view
        WHERE user_id = :userId
        AND viewed_at BETWEEN :startDate AND :endDate
        GROUP BY TRUNC(viewed_at)
        ORDER BY TRUNC(viewed_at)
        """, nativeQuery = true)
    List<Object[]> findDailyViewCountByWeek(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * 주차별 카테고리 밸런스 조회
     */
    @Query(value = """
        SELECT na.section, COUNT(*)
        FROM user_article_view uav
        JOIN naver_article na ON uav.naver_article_id = na.naver_article_id
        WHERE uav.user_id = :userId
        AND uav.viewed_at BETWEEN :startDate AND :endDate
        GROUP BY na.section
        """, nativeQuery = true)
    List<Object[]> findCategoryBalanceByWeek(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자 탈퇴 시 연관 데이터 삭제
     */
    void deleteByUserUserId(Long userId);

    boolean existsByUserUserIdAndArticleId(Long userId, Long articleId);

    // 유저별 기간동안 학습한 뉴스 수 조회
    @Query("SELECT uav.user.userId, COUNT(uav) FROM UserArticleViewEntity uav " +
            "WHERE uav.viewedAt BETWEEN :start AND :end " +
            "GROUP BY uav.user.userId")
    List<Object[]> countByUserGroupedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}