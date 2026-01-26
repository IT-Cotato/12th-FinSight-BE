package com.finsight.finsight.domain.mypage.domain.service;

import com.finsight.finsight.domain.auth.exception.AuthException;
import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.domain.category.application.dto.request.SaveCategoryRequest;
import com.finsight.finsight.domain.category.domain.service.CategoryService;
import com.finsight.finsight.domain.category.persistence.entity.UserCategoryEntity;
import com.finsight.finsight.domain.category.persistence.repository.UserCategoryRepository;
import com.finsight.finsight.domain.mypage.application.dto.request.UpdateProfileRequest;
import com.finsight.finsight.domain.mypage.application.dto.response.MypageResponse;
import com.finsight.finsight.domain.mypage.exception.MypageException;
import com.finsight.finsight.domain.mypage.exception.code.MypageErrorCode;
import com.finsight.finsight.domain.mypage.persistence.mapper.MypageConverter;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final CategoryService categoryService;

    public MypageResponse.MemberProfileResponse getUserProfile(Long userId) {
        // db에서 조회에 실패한 경우
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        return MypageConverter.toMypageProfileResponse(user);

    }

    public void withdrawMember(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new MypageException(MypageErrorCode.MEMBER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 변경
        if (!user.getNickname().equals(request.nickname())) {
            validateNicknameFormat(request.nickname());
            user.updateNickname(request.nickname());
        }

        // 카테고리 업데이트 (CategoryService 재사용)
        SaveCategoryRequest saveCategoryRequest = new SaveCategoryRequest(request.categories());
        categoryService.saveCategories(userId, saveCategoryRequest);
    }

    private void validateNicknameFormat(String nickname) {
        if (nickname == null || nickname.length() < 1 || nickname.length() > 10) {
            throw new AuthException(
                    AuthErrorCode.INVALID_NICKNAME_FORMAT);
        }
    }

    @Transactional(readOnly = true)
    public void checkNickname(Long userId, String nickname) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new MypageException(MypageErrorCode.MEMBER_NOT_FOUND));

        // 본인 닉네임인 경우 중복 체크 패스
        if (user.getNickname().equals(nickname)) {
            return;
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new MypageException(MypageErrorCode.DUPLICATE_NICKNAME);
        }
    }

}
