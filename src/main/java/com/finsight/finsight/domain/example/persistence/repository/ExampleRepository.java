package com.finsight.finsight.domain.example.persistence.repository;

import com.finsight.finsight.domain.example.persistence.entity.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<ExampleEntity, Long> {
}
