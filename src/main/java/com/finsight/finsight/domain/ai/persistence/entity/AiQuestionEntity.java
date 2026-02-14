package com.finsight.finsight.domain.ai.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "ai_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_quiz_set_id", nullable = false)
    private AiQuizSetEntity quizSet;

    @Lob
    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Lob
    @Column(name = "explanation")
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiQuestionChoiceEntity> choices = new ArrayList<>();

    @Builder
    private AiQuestionEntity(String questionText, String explanation) {
        this.questionText = questionText;
        this.explanation = explanation;
    }

    void setQuizSet(AiQuizSetEntity quizSet) {
        this.quizSet = quizSet;
    }

    public void addChoice(AiQuestionChoiceEntity c) {
        choices.add(c);
        c.setQuestion(this);
    }
}
