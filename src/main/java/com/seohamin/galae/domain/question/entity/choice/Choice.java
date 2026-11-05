package com.seohamin.galae.domain.question.entity.choice;

import com.seohamin.galae.domain.question.entity.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질문의 선택지
 * choiceIndex(0, 1, 2)가 프론트의 선택지 id(sel)이다
 * 선택지마다 색을 다르게 주지 않는다 (docs/PLAN.md 8.2) - 색 정보는 두지 않음
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "choice",
        uniqueConstraints = @UniqueConstraint(name = "uk_choice_question_index", columnNames = {"question_id", "choice_index"})
)
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "choice_index", nullable = false)
    private Integer choiceIndex;

    //선택 화면에 보이는 전체 문장
    @Column(length = 200, nullable = false)
    private String text;

    //결과·공유 화면에 보이는 짧은 문장
    @Column(length = 50, nullable = false)
    private String shortText;

    @Builder
    public Choice(final Integer choiceIndex, final String text, final String shortText) {
        this.choiceIndex = choiceIndex;
        this.text = text;
        this.shortText = shortText;
    }

    public void updateQuestion(final Question question) {
        this.question = question;
    }

    public void updateText(final String text) {
        this.text = text;
    }

    public void updateShortText(final String shortText) {
        this.shortText = shortText;
    }
}
