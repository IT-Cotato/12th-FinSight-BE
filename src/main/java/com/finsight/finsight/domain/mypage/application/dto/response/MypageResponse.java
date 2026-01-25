package com.finsight.finsight.domain.mypage.application.dto.response;

import com.finsight.finsight.domain.category.persistence.entity.UserCategoryEntity;
import lombok.Builder;

import java.util.List;

public class MypageResponse {
    @Builder
    public record MemberProfileResponse(
            String nickname,
            List<String> userCategories,
            int currentLv,
            int nextLv,
            int percentLv
    ){ }
}
