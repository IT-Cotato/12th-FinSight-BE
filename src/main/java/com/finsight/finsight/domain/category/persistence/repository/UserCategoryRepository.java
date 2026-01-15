package com.finsight.finsight.domain.category.persistence.repository;

import com.finsight.finsight.domain.category.persistence.entity.UserCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCategoryRepository extends JpaRepository<UserCategoryEntity, Long> {

    List<UserCategoryEntity> findByUserUserId(Long userId);

    void deleteByUserUserId(Long userId);
}
