package com.finsight.finsight.domain.auth.persistence.repository;

import com.finsight.finsight.domain.auth.persistence.entity.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {

    // 사용자 가장 최근 인증번호 확인
    Optional<EmailVerificationEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    // 회원가입 후 인증정보 삭제
    void deleteByEmail(String email);
}