package com.finsight.finsight.domain.category.domain.service;

import com.finsight.finsight.domain.category.persistence.entity.CategoryEntity;
import com.finsight.finsight.domain.category.persistence.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("[Category] 초기 데이터 이미 존재, 스킵");
            return;
        }

        List<CategoryEntity> categories = List.of(
                CategoryEntity.builder().code("FINANCE").nameKo("금융").sortOrder(1).build(),
                CategoryEntity.builder().code("STOCK").nameKo("증권").sortOrder(2).build(),
                CategoryEntity.builder().code("INDUSTRY").nameKo("산업/재계").sortOrder(3).build(),
                CategoryEntity.builder().code("SME").nameKo("중기/벤처").sortOrder(4).build(),
                CategoryEntity.builder().code("REAL_ESTATE").nameKo("부동산").sortOrder(5).build(),
                CategoryEntity.builder().code("GLOBAL").nameKo("글로벌 경제").sortOrder(6).build(),
                CategoryEntity.builder().code("LIVING").nameKo("생활경제").sortOrder(7).build(),
                CategoryEntity.builder().code("GENERAL").nameKo("경제 일반").sortOrder(8).build()
        );

        categoryRepository.saveAll(categories);
        log.info("[Category] 초기 데이터 8개 삽입 완료");
    }
}
