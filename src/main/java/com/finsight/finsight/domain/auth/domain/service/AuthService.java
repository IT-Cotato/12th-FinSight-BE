package com.finsight.finsight.domain.auth.domain.service;

import com.finsight.finsight.domain.auth.application.dto.request.*;
import com.finsight.finsight.domain.auth.application.dto.response.KakaoLoginResponse;
import com.finsight.finsight.domain.auth.application.dto.response.KakaoTokenResponse;
import com.finsight.finsight.domain.auth.application.dto.response.KakaoUserResponse;
import com.finsight.finsight.domain.auth.application.dto.response.TokenResponse;
import com.finsight.finsight.domain.auth.exception.AuthException;
import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.domain.auth.persistence.entity.EmailVerificationEntity;
import com.finsight.finsight.domain.auth.persistence.repository.EmailVerificationRepository;
import com.finsight.finsight.domain.user.domain.constant.AuthType;
import com.finsight.finsight.domain.user.persistence.entity.UserAuthEntity;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserAuthRepository;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderRepository;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.ErrorCode;
import com.finsight.finsight.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
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
    private final FolderRepository folderRepository;

    // 회원가입 닉네임 중복 확인
    public void checkNickname(String nickname) {
        validateNicknameFormat(nickname);

        if (userRepository.existsByNickname(nickname)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
    }

    /*
    이메일 인증 코드 전송
    - 이미 가입된 이메일 검증
    - DB 저장 후 메일 발송
     */
    public void sendCode(String email) {
        validateEmailFormat(email);

        if (userAuthRepository.existsByIdentifier(email)) {
            log.warn("[AUTH] event_type=email_code_failed email={} reason=duplicate_email", email);
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        String code = emailService.generateVerificationCode();

        EmailVerificationEntity verification = EmailVerificationEntity.builder()
                .email(email)
                .verificationCode(code)
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationEmail(email, code);
        log.info("[AUTH] event_type=email_code_sent email={}", email);
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
            throw new AuthException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!verification.getVerificationCode().equals(code)) {
            throw new AuthException(AuthErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        verification.verify();
    }

    /*
    회원가입 처리
    - 이메일 인증 여부 확인
    - 닉네임 중복 확인
     */
    public void signup(SignupRequest request) {
        validateEmailFormat(request.email());
        validatePasswordFormat(request.password());
        validateNicknameFormat(request.nickname());

        // 이메일 인증 여부 확인
        EmailVerificationEntity verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.email())
                .orElseThrow(() -> new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED));

        if (!verification.isVerified()) {
            throw new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (userAuthRepository.existsByIdentifier(request.email())) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new AuthException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        UserEntity user = UserEntity.builder()
                .nickname(request.nickname())
                .build();

        userRepository.save(user);

        // 기본 폴더 생성 (NEWS, TERM)
        createDefaultFolders(user);

        UserAuthEntity userAuth = UserAuthEntity.builder()
                .user(user)
                .identifier(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authType(AuthType.EMAIL)
                .build();

        userAuthRepository.save(userAuth);
        emailVerificationRepository.deleteByEmail(request.email());
        log.info("[AUTH] event_type=signup_success user_id={} email={} auth_type=EMAIL", user.getUserId(), request.email());
    }

    /*
    로그인 처리
    - 이메일 / 비밀번호 인증
    - Access / Refresh 토큰 발급
    - Refresh 토큰 DB 저장
     */
    public TokenResponse login(LoginRequest request) {
        validateEmailFormat(request.email());

        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(request.email(), AuthType.EMAIL)
                .orElseThrow(() -> {
                    log.warn("[AUTH] event_type=login_failed email={} reason=user_not_found", request.email());
                    return new AuthException(AuthErrorCode.USER_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            log.warn("[AUTH] event_type=login_failed email={} reason=invalid_password", request.email());
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtUtil.createAccessToken(request.email());
        String refreshToken = jwtUtil.createRefreshToken(request.email());

        userAuth.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        // 출석 체크
        recordAttendance(userAuth.getUser());

        log.info("[AUTH] event_type=login_success user_id={} email={} auth_type=EMAIL", userAuth.getUser().getUserId(), request.email());
        return new TokenResponse(accessToken, refreshToken);
    }

    /*
    Access Token 재발급
    - Refresh Token 유효성 및 DB 검증
     */
    public TokenResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("[AUTH] event_type=token_refresh_failed reason=invalid_token");
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        if (jwtUtil.isExpired(refreshToken)) {
            log.warn("[AUTH] event_type=token_refresh_failed reason=expired_token");
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        }

        String identifier = jwtUtil.getEmail(refreshToken);

        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(identifier, AuthType.EMAIL)
                .or(() -> userAuthRepository.findByIdentifierAndAuthType(identifier, AuthType.KAKAO))
                .orElseThrow(() -> {
                    log.warn("[AUTH] event_type=token_refresh_failed identifier={} reason=user_not_found", identifier);
                    return new AuthException(AuthErrorCode.USER_NOT_FOUND);
                });

        if (!refreshToken.equals(userAuth.getRefreshToken())) {
            log.warn("[AUTH] event_type=token_refresh_failed identifier={} reason=token_mismatch", identifier);
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtUtil.createAccessToken(identifier);
        String newRefreshToken = jwtUtil.createRefreshToken(identifier);

        userAuth.updateRefreshToken(newRefreshToken, LocalDateTime.now().plusDays(30));

        // 출석 체크
        recordAttendance(userAuth.getUser());

        log.debug("[AUTH] event_type=token_refresh_success identifier={}", identifier);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /*
    로그아웃 처리
    - DB 내 Refresh Token 제거
     */
    public void logout(String identifier) {
        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(identifier, AuthType.EMAIL)
                .or(() -> userAuthRepository.findByIdentifierAndAuthType(identifier, AuthType.KAKAO))
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        userAuth.clearRefreshToken();
        log.info("[AUTH] event_type=logout user_id={} identifier={}", userAuth.getUser().getUserId(), identifier);
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
            log.info("[AUTH] event_type=kakao_login_new_user kakao_id={}", kakaoId);
            return KakaoLoginResponse.newUser(kakaoId);
        }

        UserAuthEntity userAuth = existingUser.get();
        Long userId = userAuth.getUser().getUserId();

        String accessToken = jwtUtil.createAccessToken(kakaoId);
        String refreshToken = jwtUtil.createRefreshToken(kakaoId);

        userAuth.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        // 출석 체크
        recordAttendance(userAuth.getUser());

        log.info("[AUTH] event_type=login_success user_id={} auth_type=KAKAO", userId);
        return KakaoLoginResponse.of(accessToken, refreshToken);
    }

    /*
    카카오 회원가입
    - 닉네임 중복 확인
    - User, UserAuth 생성
     */
    public TokenResponse kakaoSignup(String kakaoId, String nickname) {
        validateNicknameFormat(nickname);

        if (userRepository.existsByNickname(nickname)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        if (userAuthRepository.existsByIdentifier(kakaoId)) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        UserEntity user = UserEntity.builder()
                .nickname(nickname)
                .build();

        userRepository.save(user);

        // 기본 폴더 생성 (NEWS, TERM)
        createDefaultFolders(user);

        UserAuthEntity userAuth = UserAuthEntity.builder()
                .user(user)
                .identifier(kakaoId)
                .passwordHash(null)
                .authType(AuthType.KAKAO)
                .build();

        userAuthRepository.save(userAuth);

        String accessToken = jwtUtil.createAccessToken(kakaoId);
        String refreshToken = jwtUtil.createRefreshToken(kakaoId);

        userAuth.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        log.info("[AUTH] event_type=signup_success user_id={} auth_type=KAKAO", user.getUserId());
        return new TokenResponse(accessToken, refreshToken);
    }

    /*
    비밀번호 재설정 - 인증번호 발송
    - 가입된 이메일인지 확인
    - 인증번호 발송
    */
    public void sendCodeForPasswordReset(String email) {
        validateEmailFormat(email);

        // 가입된 이메일인지 확인 (회원가입과 반대)
        if (!userAuthRepository.existsByIdentifier(email)) {
            throw new AuthException(AuthErrorCode.EMAIL_NOT_FOUND);
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
    비밀번호 재설정
    - 이메일 인증 여부 확인
    - 새 비밀번호 형식 검증
    - 비밀번호 업데이트
     */
    public void resetPassword(String email, String newPassword) {
        validateEmailFormat(email);
        validatePasswordFormat(newPassword);

        // 이메일 인증 여부 확인
        EmailVerificationEntity verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED));

        if (!verification.isVerified()) {
            throw new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 사용자 찾기
        UserAuthEntity userAuth = userAuthRepository
                .findByIdentifierAndAuthType(email, AuthType.EMAIL)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 비밀번호 업데이트
        userAuth.updatePassword(passwordEncoder.encode(newPassword));

        // 인증 기록 삭제
        emailVerificationRepository.deleteByEmail(email);
        log.info("[AUTH] event_type=password_reset_success user_id={} email={}", userAuth.getUser().getUserId(), email);
    }

    // 닉네임 형식 검증 (1-10자)
    private void validateNicknameFormat(String nickname) {
        if (nickname.length() < 1 || nickname.length() > 10) {
            throw new AuthException(AuthErrorCode.INVALID_NICKNAME_FORMAT);
        }
    }

    // 이메일 형식 검증
    private void validateEmailFormat(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!email.matches(emailRegex)) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    // 비밀번호 형식 검증 (영문+숫자 6-18자)
    private void validatePasswordFormat(String password) {
        String passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,18}$";
        if (!password.matches(passwordRegex)) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    // 기본 폴더 생성 (NEWS, TERM)
    private void createDefaultFolders(UserEntity user) {
        FolderEntity newsFolder = FolderEntity.builder()
                .user(user)
                .folderType(FolderType.NEWS)
                .folderName("기본")
                .sortOrder(1)
                .build();
        folderRepository.save(newsFolder);

        FolderEntity termFolder = FolderEntity.builder()
                .user(user)
                .folderType(FolderType.TERM)
                .folderName("기본")
                .sortOrder(1)
                .build();
        folderRepository.save(termFolder);
    }

    /**
     * 출석 체크 (로그인 시 호출)
     * UserEntity의 출석 카운트 증가
     */
    private void recordAttendance(UserEntity user) {
        user.incrementAttendance();
        userRepository.save(user);
    }
}