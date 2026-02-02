package com.finsight.finsight.domain.storage.persistence.repository;

import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<FolderEntity, Long> {

    // 사용자의 폴더 목록 조회 (타입별, 순서대로)
    List<FolderEntity> findByUserUserIdAndFolderTypeOrderBySortOrderAsc(Long userId, FolderType folderType);

    // 폴더 조회 (사용자 권한 체크 포함)
    Optional<FolderEntity> findByFolderIdAndUserUserId(Long folderId, Long userId);

    // 폴더 개수 조회
    long countByUserUserIdAndFolderType(Long userId, FolderType folderType);

    // 폴더명 중복 체크
    boolean existsByUserUserIdAndFolderTypeAndFolderName(Long userId, FolderType folderType, String folderName);

    /** 사용자 탈퇴 시 연관 데이터 삭제 */
    void deleteByUserUserId(Long userId);
}
