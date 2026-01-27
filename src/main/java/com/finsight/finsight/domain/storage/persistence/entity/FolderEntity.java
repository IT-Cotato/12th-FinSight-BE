package com.finsight.finsight.domain.storage.persistence.entity;

import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Builder.Default
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FolderItemEntity> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "folder_type", nullable = false, length = 20)
    private FolderType folderType;

    @Column(name = "folder_name", nullable = false, length = 100)
    private String folderName;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
