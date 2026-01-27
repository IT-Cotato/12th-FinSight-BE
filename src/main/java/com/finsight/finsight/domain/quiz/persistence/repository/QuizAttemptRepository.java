package com.finsight.finsight.domain.quiz.persistence.repository;

import com.finsight.finsight.domain.quiz.persistence.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    
    /** 사용자의 특정 퀴즈 세트에 대한 풀이 기록 조회 */
    Optional<QuizAttemptEntity> findByUserUserIdAndQuizSetId(Long userId, Long quizSetId);
}
