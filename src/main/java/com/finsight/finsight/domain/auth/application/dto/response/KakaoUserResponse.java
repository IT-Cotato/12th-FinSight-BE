package com.finsight.finsight.domain.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
        public record Profile(
                String nickname,

                @JsonProperty("profile_image_url")
                String profileImageUrl
        ) {}
    }
}