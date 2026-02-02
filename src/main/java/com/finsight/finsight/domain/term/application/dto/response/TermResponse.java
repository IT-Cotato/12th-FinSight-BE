package com.finsight.finsight.domain.term.application.dto.response;

import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermResponse {

    private Long termId;
    private String name;
    private String description;

    public static TermResponse from(TermEntity term) {
        return TermResponse.builder()
                .termId(term.getId())
                .name(term.getDisplayName())
                .description(term.getDefinition())
                .build();
    }
}
