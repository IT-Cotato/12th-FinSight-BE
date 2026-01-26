package com.finsight.finsight.domain.category.persistence.repository;

import com.finsight.finsight.domain.category.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    // 기본 순서대로 전체 조회
    List<CategoryEntity> findAllByOrderBySortOrderAsc();

    // 코드로 조회
    Optional<CategoryEntity> findByCode(String code);

    // 코드 존재 여부
    boolean existsByCode(String code);
}
