package com.finsight.finsight.domain.quiz.persistence.repository;

import com.finsight.finsight.domain.quiz.persistence.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 일일퀘스트 : 오늘 '신규'로 퀴즈를 풀었는지 확인
    // 오늘 풀었으며(attemptedAt), 해당 기록이 오늘 처음 생성된 경우(createdAt == attemptedAt)를 찾습니다.
    @Query("SELECT COUNT(qa) > 0 FROM QuizAttemptEntity qa WHERE qa.user.userId = :userId AND qa.attemptedAt >= :startOfToday AND qa.createdAt >= :startOfToday")
    boolean existsNewAttemptToday(@Param("userId") Long userId, @Param("startOfToday") LocalDateTime startOfToday);

    // 일일퀘스트 : 오늘 '복습'을 완료했는지 확인
    // 오늘 다시 풀었지만(attemptedAt), 실제 데이터 생성일(createdAt)은 오늘 이전인 경우입니다.
    @Query("SELECT COUNT(qa) > 0 FROM QuizAttemptEntity qa WHERE qa.user.userId = :userId AND qa.attemptedAt >= :startOfToday AND qa.createdAt < :startOfToday")
    boolean existsReviewAttemptToday(@Param("userId") Long userId, @Param("startOfToday") LocalDateTime startOfToday);
}
