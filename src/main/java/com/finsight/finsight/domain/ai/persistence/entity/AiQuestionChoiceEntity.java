package com.finsight.finsight.domain.ai.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "ai_question_choices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuestionChoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_question_choice_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_question_id", nullable = false)
    private AiQuestionEntity question;

    @Column(name = "choice_text", nullable = false, length = 2000)
    private String choiceText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Builder
    private AiQuestionChoiceEntity(String choiceText, boolean correct) {
        this.choiceText = choiceText;
        this.correct = correct;
    }

    void setQuestion(AiQuestionEntity question) {
        this.question = question;
    }
}
