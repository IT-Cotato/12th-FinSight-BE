package com.finsight.finsight.domain.storage.persistence.repository;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderItemEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FolderItemRepository extends JpaRepository<FolderItemEntity, Long> {

    // 특정 폴더의 아이템 목록 조회 (최신순)
    Page<FolderItemEntity> findByFolderFolderIdOrderBySavedAtDesc(Long folderId, Pageable pageable);

    // 특정 폴더에 특정 아이템 존재 여부
    boolean existsByFolderFolderIdAndItemTypeAndItemId(Long folderId, FolderType itemType, Long itemId);

    // 특정 아이템이 저장된 폴더 목록 조회
    @Query("SELECT fi FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType AND fi.itemId = :itemId")
    List<FolderItemEntity> findByUserIdAndItemTypeAndItemId(@Param("userId") Long userId, @Param("itemType") FolderType itemType, @Param("itemId") Long itemId);

    // 특정 폴더의 특정 아이템 조회
    Optional<FolderItemEntity> findByFolderFolderIdAndItemTypeAndItemId(Long folderId, FolderType itemType, Long itemId);

    // 사용자의 모든 폴더에서 특정 타입 아이템 목록 조회 (최신순)
    @Query("SELECT fi FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType ORDER BY fi.savedAt DESC")
    Page<FolderItemEntity> findByUserIdAndItemType(@Param("userId") Long userId, @Param("itemType") FolderType itemType, Pageable pageable);

    // ========== 뉴스 보관함용 추가 메서드 ==========

    // 중복 저장 체크 (사용자의 모든 폴더에서)
    @Query("SELECT COUNT(fi) > 0 FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType AND fi.itemId = :itemId")
    boolean existsByUserIdAndItemTypeAndItemId(@Param("userId") Long userId, @Param("itemType") FolderType itemType, @Param("itemId") Long itemId);

    // 폴더별 뉴스 조회
    Page<FolderItemEntity> findByFolderAndItemType(FolderEntity folder, FolderType itemType, Pageable pageable);

    // 섹션 필터링 포함 조회 (JOIN 쿼리)
    @Query("""
        SELECT f.folderItemId, n.id, n.title, n.press, n.section,
               n.thumbnailUrl, n.publishedAt, f.savedAt
        FROM FolderItemEntity f
        JOIN NaverArticleEntity n ON f.itemId = n.id
        WHERE f.folder = :folder
          AND f.itemType = 'NEWS'
          AND (:section IS NULL OR n.section = :section)
        ORDER BY f.savedAt DESC
    """)
    Page<Object[]> findSavedNewsWithFilter(
            @Param("folder") FolderEntity folder,
            @Param("section") NaverEconomySection section,
            Pageable pageable);

    // 검색 (JOIN 쿼리)
    @Query("""
        SELECT f.folderItemId, n.id, n.title, n.press, n.section,
               n.thumbnailUrl, n.publishedAt, f.savedAt
        FROM FolderItemEntity f
        JOIN NaverArticleEntity n ON f.itemId = n.id
        WHERE f.folder.user.userId = :userId
          AND f.itemType = 'NEWS'
          AND (n.title LIKE %:query% OR n.content LIKE %:query%)
        ORDER BY f.savedAt DESC
    """)
    Page<Object[]> searchSavedNewsByQuery(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable);

    /**
     * 사용자가 저장한 특정 타입의 아이템 개수
     */
    @Query("SELECT COUNT(fi) FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType")
    Long countByUserIdAndItemType(
                        @Param("userId") Long userId,
                        @Param("itemType") FolderType itemType);

   /**
    * 주차별 저장된 아이템 목록 조회 (JPQL)
    */
    @Query("SELECT fi FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType AND fi.savedAt BETWEEN :startDate AND :endDate")
    List<FolderItemEntity> findSavedItemsByWeek(
                        @Param("userId") Long userId,
                        @Param("itemType") FolderType itemType,
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 주차별 카테고리별 저장된 뉴스 개수 (학습 리포트용)
     */
    @Query(value = "SELECT n.section, COUNT(*) as count " +
                        "FROM folder_items f " +
                        "JOIN naver_articles n ON f.item_id = n.id " +
                        "WHERE f.folder_id IN (SELECT folder_id FROM folders WHERE user_id = :userId) " +
                        "AND f.item_type = 'NEWS' " +
                        "AND f.saved_at BETWEEN :startDate AND :endDate " +
                        "GROUP BY n.section " +
                        "ORDER BY count DESC", nativeQuery = true)
    List<Object[]> findSavedNewsCategoryBalanceByWeek(
                        @Param("userId") Long userId,
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

    // 폴더 수정용 - 해당 뉴스의 모든 연결 삭제
    @Modifying
    @Query("DELETE FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType AND fi.itemId = :itemId")
    void deleteByUserIdAndItemTypeAndItemId(@Param("userId") Long userId, @Param("itemType") FolderType itemType, @Param("itemId") Long itemId);

    // ========== 용어 보관함용 추가 메서드 ==========

    // 용어 목록 조회 (JOIN 쿼리)
    @Query("""
        SELECT f.folderItemId, t.id, t.displayName, t.definition, f.savedAt
        FROM FolderItemEntity f
        JOIN TermEntity t ON f.itemId = t.id
        WHERE f.folder = :folder
          AND f.itemType = 'TERM'
        ORDER BY f.savedAt DESC
    """)
    Page<Object[]> findSavedTermsByFolder(
            @Param("folder") FolderEntity folder,
            Pageable pageable);

    // 용어 검색 (JOIN 쿼리)
    @Query("""
        SELECT f.folderItemId, t.id, t.displayName, t.definition, f.savedAt
        FROM FolderItemEntity f
        JOIN TermEntity t ON f.itemId = t.id
        WHERE f.folder.user.userId = :userId
          AND f.itemType = 'TERM'
          AND t.displayName LIKE %:query%
        ORDER BY f.savedAt DESC
    """)
    Page<Object[]> searchSavedTermsByQuery(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable);

    // ========== 일일퀘스트 ==========
    // 오늘 뉴스 저장 여부 확인
    // savedAt이 특정 시점(오늘 00시) 이후인 기록이 있는지 확인합니다.
    @Query("SELECT COUNT(fi) > 0 FROM FolderItemEntity fi WHERE fi.folder.user.userId = :userId AND fi.itemType = :itemType AND fi.savedAt >= :since")
    boolean existsByUserIdAndItemTypeAndSavedAtAfter(@Param("userId") Long userId, @Param("itemType") FolderType itemType, @Param("since") LocalDateTime since);
}
