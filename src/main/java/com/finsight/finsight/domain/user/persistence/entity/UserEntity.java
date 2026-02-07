package com.finsight.finsight.domain.user.persistence.entity;

import com.finsight.finsight.domain.user.domain.constant.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String nickname;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAuthEntity> userAuths = new ArrayList<>();

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(name = "level_no", nullable = false)
    private Integer level = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer exp = 0;

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate;

    @Builder.Default
    @Column(name = "attendance_count", nullable = false)
    private Long attendanceCount = 0L;

    @Builder.Default
    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateNotificationEnabled(Boolean enabled) {
        this.notificationEnabled = enabled;
    }

    /**
     * 경험치 추가 및 레벨 계산
     * 레벨 N→N+1 필요 경험치: N × 100
     */
    public void addExp(int amount) {
        this.exp += amount;
        
        int requiredExp = this.level * 100;
        if (this.exp >= requiredExp) {
            this.exp -= requiredExp;
            this.level++;
        }
    }

    /**
     * 로그인 시 출석 체크
     * 오늘 날짜가 마지막 출석 날짜와 다르면 출석 카운트 증가
     */
    public void incrementAttendance() {
        LocalDate today = LocalDate.now();
        if (this.lastAttendanceDate == null || !this.lastAttendanceDate.equals(today)) {
            this.lastAttendanceDate = today;
            this.attendanceCount++;
        }
    }

    @PreUpdate
    protected void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}