package com.finsight.finsight.domain.quiz.persistence.repository;

import com.finsight.finsight.domain.quiz.persistence.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    
    /** 사용자의 특정 퀴즈 세트에 대한 풀이 기록 조회 */
    Optional<QuizAttemptEntity> findByUserUserIdAndQuizSetId(Long userId, Long quizSetId);

    /** 사용자가 특정 기사에 연결된 퀴즈를 하나라도 풀었는지 확인 */
    @Query("SELECT COUNT(qa) > 0 FROM QuizAttemptEntity qa WHERE qa.user.userId = :userId AND qa.quizSet.article.id = :articleId")
    boolean existsSolvedQuizByArticle(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /** N+1 방지를 위해 여러 기사 ID에 대한 풀이 완료된 기사 ID 목록 조회 */
    @Query("SELECT DISTINCT qa.quizSet.article.id FROM QuizAttemptEntity qa WHERE qa.user.userId = :userId AND qa.quizSet.article.id IN :articleIds")
    List<Long> findSolvedArticleIds(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);
}
