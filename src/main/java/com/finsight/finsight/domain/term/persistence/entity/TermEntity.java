package com.finsight.finsight.domain.term.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "terms",
        uniqueConstraints = @UniqueConstraint(name = "uk_terms_normalized", columnNames = {"normalized"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName; // 화면에 보여줄 원문(예: "CPI")

    @Column(name = "normalized", nullable = false, length = 200)
    private String normalized;  // 중복 판단용(예: "cpi" / "기준금리")

    @Lob
    @Column(name = "definition") // nullable은 일단 기본(true) 추천
    private String definition;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private TermEntity(String displayName, String normalized, String definition) {
        this.displayName = displayName;
        this.normalized = normalized;
        this.definition = definition;
        this.createdAt = LocalDateTime.now();
    }

    public void updateDefinitionIfBlank(String newDefinition) {
        if ((this.definition == null || this.definition.isBlank()) && newDefinition != null && !newDefinition.isBlank()) {
            this.definition = newDefinition;
        }
    }
}
