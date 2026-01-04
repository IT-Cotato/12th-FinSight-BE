package com.finsight.finsight.domain.auth.domain.service;

import com.finsight.finsight.domain.auth.application.dto.request.*;
import com.finsight.finsight.domain.auth.application.dto.response.KakaoLoginResponse;
import com.finsight.finsight.domain.auth.application.dto.response.KakaoTokenResponse;
import com.finsight.finsight.domain.auth.application.dto.response.KakaoUserResponse;
import com.finsight.finsight.domain.auth.domain.service.EmailService;
import com.finsight.finsight.domain.auth.application.dto.response.TokenResponse;
import com.finsight.finsight.domain.auth.persistence.entity.EmailVerificationEntity;
import com.finsight.finsight.domain.auth.persistence.repository.EmailVerificationRepository;
import com.finsight.finsight.domain.user.domain.constant.AuthType;
import com.finsight.finsight.domain.user.persistence.entity.UserAuthEntity;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserAuthRepository;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.ErrorCode;
import com.finsight.finsight.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final KakaoService kakaoService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    // 회원가입 닉네임 중복 확인
    public void checkNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    /*
    이메일 인증 코드 전송
    - 이미 가입된 이메일 검증
    - DB 저장 후 메일 발송
     */
    public void sendCode(String email) {
        if (userAuthRepository.existsByIdentifier(email)) {
            throw new AppException(ErrorCode.DUPLICATE_EMAIL);
        }

        String code = emailService.generateVerificationCode();

        EmailVerificationEntity verification = EmailVerificationEntity.builder()
                .email(email)
                .verificationCode(code)
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationEmail(email, code);
    }

    /*
    이메일 인증 코드 검증
    - DB 내의 최신 인증 코드를 기준으로 확인
    - 만료 여부 및 일치 여부 확인
     */
    public void verifyCode(String email, String code) {
        EmailVerificationEntity verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (verification.isExpired()) {
            throw new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!verification.getVerificationCode().equals(code)) {
            throw new AppException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        verification.verify();
    }

    /*
    회원가입 처리
    - 닉네임 중복 확인
     */
    public void signup(SignupRequest request) {
        if (userRepository.existsByNickname(request.nickname())) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME);
        }

        UserEntity user = UserEntity.builder()
                .nickname(request.nickname())
                .build();

        userRepository.save(user);

        UserAuthEntity userAuth = UserAuthEntity.builder()
                .userId(user.getUserId())
                .identifier(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authType(AuthType.EMAIL)
                .build();

        userAuthRepository.save(userAuth);
        emailVerificationRepository.deleteByEmail(request.email());
    }

    /*
    로그인 처리
    - 이메일 / 비밀번호 인증
    - Access / Refresh 토큰 발급
    - Refresh 토큰 DB 저장
     */
    public TokenResponse login(LoginRequest request) {
        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(request.email(), AuthType.EMAIL)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.createAccessToken(request.email());
        String refreshToken = jwtUtil.createRefreshToken(request.email());

        userAuth.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        return new TokenResponse(accessToken, refreshToken);
    }

    /*
    Access Token 재발급
    - Refresh Token 유효성 및 DB 검증
     */
    public TokenResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        if (jwtUtil.isExpired(refreshToken)) {
            throw new AppException(ErrorCode.EXPIRED_TOKEN);
        }

        String email = jwtUtil.getEmail(refreshToken);

        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(email, AuthType.EMAIL)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!refreshToken.equals(userAuth.getRefreshToken())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtUtil.createAccessToken(email);
        String newRefreshToken = jwtUtil.createRefreshToken(email);

        userAuth.updateRefreshToken(newRefreshToken, LocalDateTime.now().plusDays(30));

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /*
    로그아웃 처리
    - DB 내 Refresh Token 제거
     */
    public void logout(String email) {
        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(email, AuthType.EMAIL)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userAuth.clearRefreshToken();
    }

    /*
카카오 로그인
- 이미 가입된 사용자: 토큰 발급
- 미가입 사용자: null 반환 (회원가입 필요)
 */
    public KakaoLoginResponse kakaoLogin(String code) {
        KakaoTokenResponse kakaoToken = kakaoService.getToken(code);
        KakaoUserResponse kakaoUser = kakaoService.getUserInfo(kakaoToken.accessToken());

        String kakaoId = String.valueOf(kakaoUser.id());

        Optional<UserAuthEntity> existingUser = userAuthRepository
                .findByIdentifierAndAuthType(kakaoId, AuthType.KAKAO);

        if (existingUser.isEmpty()) {
            return KakaoLoginResponse.newUser(kakaoId);
        }

        UserAuthEntity userAuth = existingUser.get();

        String accessToken = jwtUtil.createAccessToken(kakaoId);
        String refreshToken = jwtUtil.createRefreshToken(kakaoId);

        userAuth.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        return KakaoLoginResponse.of(accessToken, refreshToken);
    }

    /*
    카카오 회원가입
    - 닉네임 중복 확인
    - User, UserAuth 생성
     */
    public TokenResponse kakaoSignup(String kakaoId, String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME);
        }

        if (userAuthRepository.existsByIdentifier(kakaoId)) {
            throw new AppException(ErrorCode.DUPLICATE_EMAIL);
        }

        UserEntity user = UserEntity.builder()
                .nickname(nickname)
                .build();

        userRepository.save(user);

        UserAuthEntity userAuth = UserAuthEntity.builder()
                .userId(user.getUserId())
                .identifier(kakaoId)
                .passwordHash(null)
                .authType(AuthType.KAKAO)
                .build();

        userAuthRepository.save(userAuth);

        String accessToken = jwtUtil.createAccessToken(kakaoId);
        String refreshToken = jwtUtil.createRefreshToken(kakaoId);

        userAuth.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        return new TokenResponse(accessToken, refreshToken);
    }
}