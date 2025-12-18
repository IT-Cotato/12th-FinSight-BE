package com.finsight.finsight.domain.example.domain.service;

import com.finsight.finsight.domain.example.application.dto.response.ExampleResponse;
import com.finsight.finsight.domain.example.persistence.entity.ExampleEntity;
import com.finsight.finsight.domain.example.persistence.repository.ExampleRepository;
import com.finsight.finsight.global.exception.EntityNotFoundException;
import com.finsight.finsight.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExampleService {

    private final ExampleRepository exampleRepository;

    @Transactional
    public Long save(String name) {
        ExampleEntity example = ExampleEntity.builder()
                .name(name)
                .build();
        return exampleRepository.save(example).getId();
    }

    public ExampleResponse findById(Long id) {
        return ExampleResponse.from(
                exampleRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOT_FOUND))
        );
    }
}
