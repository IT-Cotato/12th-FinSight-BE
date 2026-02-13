package com.finsight.finsight.domain.notification.persistence.repository;

import com.finsight.finsight.domain.notification.persistence.entity.FcmTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmTokenEntity, Long> {

    // 유저의 모든 토큰 조회
    List<FcmTokenEntity> findByUserUserId(Long userId);

    // 여러 유저의 토큰 한번에 조회 (N+1 방지)
    List<FcmTokenEntity> findByUserUserIdIn(List<Long> userIds);

    // 토큰으로 조회
    Optional<FcmTokenEntity> findByFcmToken(String fcmToken);

    // 토큰 존재 여부
    boolean existsByFcmToken(String fcmToken);

    // 토큰 삭제
    @Modifying
    void deleteByFcmToken(String fcmToken);

    // 유저의 모든 토큰 삭제
    @Modifying
    void deleteByUserUserId(Long userId);
}