package com.finsight.finsight.domain.quiz.persistence.entity;

import com.finsight.finsight.domain.ai.persistence.entity.AiQuizSetEntity;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 퀴즈 풀이 기록 엔티티
 */
@Entity
@Getter
@Table(name = "quiz_attempt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_attempt_id")
    private Long id;

    /** 푼 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 퀴즈 세트 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_set_id", nullable = false)
    private AiQuizSetEntity quizSet;

    /** 답안 JSON (예: [{"questionIndex":0,"selectedIndex":1,"correct":false}, ...]) */
    @Lob
    @Column(name = "answers_json", nullable = false)
    private String answersJson;

    /** 맞춘 개수 (0~3) */
    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    /** 획득 점수 (최대 50점) */
    @Column(name = "score", nullable = false)
    private Integer score;

    /** 최초 풀이 시각 (생성 시 고정) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 풀이 시각 (복습시 업데이트) */
    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Builder
    private QuizAttemptEntity(UserEntity user, AiQuizSetEntity quizSet, 
                              String answersJson, Integer correctCount, Integer score) {
        this.user = user;
        this.quizSet = quizSet;
        this.answersJson = answersJson;
        this.correctCount = correctCount;
        this.score = score;
        this.createdAt = LocalDateTime.now();
        this.attemptedAt = LocalDateTime.now();
    }

    /** 복습 시 기록 업데이트 */
    public void updateAttempt(String answersJson, Integer correctCount, Integer score) {
        this.answersJson = answersJson;
        this.correctCount = correctCount;
        this.score = score;
        this.attemptedAt = LocalDateTime.now();
    }
}
