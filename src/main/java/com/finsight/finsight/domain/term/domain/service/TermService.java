package com.finsight.finsight.domain.term.domain.service;

import com.finsight.finsight.domain.term.domain.TermNormalizer;
import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import com.finsight.finsight.domain.term.persistence.repository.TermRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TermService {

    private final TermRepository termRepository;

    @Transactional
    public TermEntity getOrCreate(String rawDisplayName, String definition) {
        String normalized = TermNormalizer.normalize(rawDisplayName);

        return termRepository.findByNormalized(normalized)
                .map(existing -> {
                    existing.updateDefinitionIfBlank(definition); // 기존 비었으면만 채움
                    return existing;
                })
                .orElseGet(() -> createOrLoadRace(rawDisplayName, normalized, definition));
    }

    private TermEntity createOrLoadRace(String displayName, String normalized, String definition) {
        try {
            return termRepository.save(TermEntity.builder()
                    .displayName(displayName.trim())
                    .normalized(normalized)
                    .definition(definition)
                    .build());
        } catch (DataIntegrityViolationException dup) {
            // 동시성 레이스: 누가 먼저 만들었으면 다시 조회
            return termRepository.findByNormalized(normalized)
                    .orElseThrow(() -> dup);
        }
    }
}
