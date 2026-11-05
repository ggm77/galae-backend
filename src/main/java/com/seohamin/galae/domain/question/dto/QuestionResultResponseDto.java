package com.seohamin.galae.domain.question.dto;

import com.seohamin.galae.domain.question.entity.choice.Choice;
import lombok.Getter;

import java.util.List;

/**
 * 결과 화면: 선택별 비율 막대
 * 내 답을 남긴 사람만 받을 수 있다
 */
@Getter
public class QuestionResultResponseDto {

    private final String questionId;
    private final Long participantCount;
    private final Integer myChoiceId;
    private final List<ChoiceResult> choices;

    public QuestionResultResponseDto(
            final String questionId,
            final long participantCount,
            final Integer myChoiceId,
            final List<ChoiceResult> choices
    ) {
        this.questionId = questionId;
        this.participantCount = participantCount;
        this.myChoiceId = myChoiceId;
        this.choices = choices;
    }

    @Getter
    public static class ChoiceResult {
        private final Integer id;
        private final String text;
        private final String shortText;
        private final Long count;
        //정수 비율. 합이 100이 되도록 맞춘다
        private final Integer pct;

        public ChoiceResult(final Choice choice, final long count, final int pct) {
            this.id = choice.getChoiceIndex();
            this.text = choice.getText();
            this.shortText = choice.getShortText();
            this.count = count;
            this.pct = pct;
        }
    }
}
