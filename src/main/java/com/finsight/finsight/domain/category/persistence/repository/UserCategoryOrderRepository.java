package com.finsight.finsight.domain.category.persistence.repository;

import com.finsight.finsight.domain.category.persistence.entity.UserCategoryOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface UserCategoryOrderRepository extends JpaRepository<UserCategoryOrderEntity, Long> {

    // 사용자의 카테고리 순서 목록 조회 (순서대로)
    List<UserCategoryOrderEntity> findByUserUserIdOrderBySortOrderAsc(Long userId);

    // 사용자의 특정 카테고리 순서 조회
    Optional<UserCategoryOrderEntity> findByUserUserIdAndCategoryCategoryId(Long userId, Long categoryId);

    // 사용자의 카테고리 순서 존재 여부
    boolean existsByUserUserId(Long userId);

    // 사용자의 카테고리 순서 전체 삭제
    @Modifying
    void deleteByUserUserId(Long userId);
}
