package com.finsight.finsight.domain.user.persistence.repository;

import com.finsight.finsight.domain.user.domain.constant.AuthType;
import com.finsight.finsight.domain.user.persistence.entity.UserAuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuthEntity, Long> {

    // 로그인 사용자 조회
    Optional<UserAuthEntity> findByIdentifierAndAuthType(String identifier, AuthType authType);

    // 회원가입 이메일 중복 확인
    boolean existsByIdentifier(String identifier);
}