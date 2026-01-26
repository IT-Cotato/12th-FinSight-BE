package com.finsight.finsight.domain.category.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;  // 'FINANCE', 'STOCK' 등

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;  // '금융', '증권' 등

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;  // 기본 순서 1~8

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
}
