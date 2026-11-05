package com.seohamin.galae.domain.question.dto;

import com.seohamin.galae.domain.answer.entity.Answer;
import com.seohamin.galae.domain.question.entity.Question;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 질문 목록(질문 탭)의 한 줄
 */
@Getter
public class QuestionSummaryResponseDto {

    private final String id;
    private final LocalDate date;
    private final String title;
    private final String body;
    private final Long participantCount;
    //아직 답하지 않았으면 null
    private final MyAnswerStatus myAnswer;

    public QuestionSummaryResponseDto(final Question question, final long participantCount, final Answer answer) {
        this.id = question.getId();
        this.date = question.getPublishDate();
        this.title = question.getTitle();
        this.body = question.getBody();
        this.participantCount = participantCount;
        this.myAnswer = answer == null ? null : new MyAnswerStatus(answer);
    }

    @Getter
    public static class MyAnswerStatus {
        private final Integer choiceId;
        private final Boolean completed;
        private final LocalDate answeredOn;
        private final String lastStep;

        public MyAnswerStatus(final Answer answer) {
            this.choiceId = answer.getChoice().getChoiceIndex();
            this.completed = answer.getCompleted();
            this.answeredOn = answer.getAnsweredOn();
            this.lastStep = answer.getLastStep();
        }
    }
}
