package com.finsight.finsight.domain.naver.domain.service;

import com.finsight.finsight.domain.naver.application.dto.response.*;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.global.exception.AppException;
import com.finsight.finsight.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class NaverArticleQueryService {

    private final NaverArticleRepository naverArticleRepository;

    public NaverArticleDetailResponse detail(Long articleId) {
        var a = naverArticleRepository.findById(articleId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return NaverArticleDetailResponse.from(a);
    }
}
