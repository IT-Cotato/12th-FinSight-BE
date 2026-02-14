package com.finsight.finsight.domain.term.domain.service;

import com.finsight.finsight.domain.term.application.dto.response.TermResponse;
import com.finsight.finsight.domain.term.exception.TermException;
import com.finsight.finsight.domain.term.exception.code.TermErrorCode;
import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import com.finsight.finsight.domain.term.persistence.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermQueryService {

    private final TermRepository termRepository;

    public TermResponse getTerm(Long termId) {
        TermEntity term = termRepository.findById(termId)
                .orElseThrow(() -> new TermException(TermErrorCode.TREM_NOT_FOUND));

        return TermResponse.from(term);
    }
}
