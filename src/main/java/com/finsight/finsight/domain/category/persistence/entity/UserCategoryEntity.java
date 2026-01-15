package com.finsight.finsight.domain.category.persistence.entity;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Table(name = "user_interests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interest_id")
    private Long interestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NaverEconomySection section;

    public static UserCategoryEntity of(UserEntity user, NaverEconomySection section) {
        return UserCategoryEntity.builder()
                .user(user)
                .section(section)
                .build();
    }
}
