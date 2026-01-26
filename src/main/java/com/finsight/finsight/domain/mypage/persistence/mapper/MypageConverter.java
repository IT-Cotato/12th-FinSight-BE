package com.finsight.finsight.domain.mypage.persistence.mapper;

import com.finsight.finsight.domain.category.persistence.entity.UserCategoryEntity;
import com.finsight.finsight.domain.mypage.application.dto.response.MypageResponse;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;

import java.util.List;
import java.util.stream.Collectors;

public class MypageConverter {

    // 엔티티 -> dto
    public static MypageResponse.MemberProfileResponse toMypageProfileResponse(UserEntity userEntity){
        return MypageResponse.MemberProfileResponse.builder()
                .nickname(userEntity.getNickname())
                .currentLv(userEntity.getLevel())
                .percentLv(userEntity.getExp() / userEntity.getLevel())
                .nextLv(userEntity.getLevel() + 1)
                .build();
    }
}
