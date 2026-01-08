package com.finsight.finsight.domain.term.persistence.repository;

import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<TermEntity, Long> {
    Optional<TermEntity> findByNormalized(String normalized);
}
