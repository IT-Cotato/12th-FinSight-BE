package com.finsight.finsight.domain.user.persistence.repository;

import com.finsight.finsight.domain.user.domain.constant.AuthType;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 닉네임 중복 확인
    boolean existsByNickname(String nickname);

    UserEntity findUserEntityByUserId(Long userId);

    /**
     * 알림 활성화된 EMAIL 유저 목록 조회 (email 알림 전용)
     */
    @Query("""
        SELECT u FROM UserEntity u
        JOIN u.userAuths ua
        WHERE u.notificationEnabled = true
        AND ua.authType = :authType
    """)
    List<UserEntity> findByNotificationEnabledAndAuthType(@Param("authType") AuthType authType);

    // 알림 활성화된 유저 조회 (fcm 전용)
    List<UserEntity> findByNotificationEnabledTrue();
}