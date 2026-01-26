package com.finsight.finsight.domain.user.persistence.repository;

import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 닉네임 중복 확인
    boolean existsByNickname(String nickname);

    UserEntity findUserEntityByUserId(Long userId);
}