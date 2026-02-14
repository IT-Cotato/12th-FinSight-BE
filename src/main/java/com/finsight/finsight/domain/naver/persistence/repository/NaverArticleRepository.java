package com.finsight.finsight.domain.naver.persistence.repository;

import com.finsight.finsight.domain.naver.domain.service.ArticleQueryDsl;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NaverArticleRepository extends JpaRepository<NaverArticleEntity, Long>, ArticleQueryDsl {
    boolean existsByOidAndAid(String oid, String aid);
}
