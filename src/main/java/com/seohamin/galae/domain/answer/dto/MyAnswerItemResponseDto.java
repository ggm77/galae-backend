package com.seohamin.galae.domain.answer.dto;

import com.seohamin.galae.domain.answer.entity.Answer;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 나 탭 달력: 고른 날의 답변
 */
@Getter
public class MyAnswerItemResponseDto {

    private final String questionId;
    private final LocalDate questionDate;
    private final String title;
    private final Integer choiceId;
    private final String choiceShortText;
    private final String reason;
    private final LocalDate answeredOn;
    private final Boolean completed;

    public MyAnswerItemResponseDto(final Answer answer) {
        this.questionId = answer.getQuestion().getId();
        this.questionDate = answer.getQuestion().getPublishDate();
        this.title = answer.getQuestion().getTitle();
        this.choiceId = answer.getChoice().getChoiceIndex();
        this.choiceShortText = answer.getChoice().getShortText();
        this.reason = answer.getReason();
        this.answeredOn = answer.getAnsweredOn();
        this.completed = answer.getCompleted();
    }
}
