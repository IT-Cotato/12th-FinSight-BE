package com.finsight.finsight.domain.ai.application.dto.response;

import com.finsight.finsight.domain.ai.persistence.entity.AiTermCardEntity;

public record TermCardResponse(
        Long termId,
        String term,
        String definition,
        String highlightText,
        int cardOrder
) {
    public static TermCardResponse from(AiTermCardEntity e) {
        return new TermCardResponse(
                e.getTerm().getId(),
                e.getTerm().getDisplayName(),
                e.getTerm().getDefinition(),
                e.getHighlightText(),
                e.getCardOrder()
        );
    }
}
