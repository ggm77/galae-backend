package com.seohamin.galae.domain.question.dto;

import com.seohamin.galae.domain.answer.dto.AnswerResponseDto;
import com.seohamin.galae.domain.answer.entity.Answer;
import com.seohamin.galae.domain.question.entity.Question;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 질문 상세 (선택 화면)
 */
@Getter
public class QuestionResponseDto {

    private final String id;
    private final LocalDate date;
    private final String title;
    private final String body;
    private final Long participantCount;
    private final List<ChoiceResponseDto> choices;
    //아직 답하지 않았으면 null
    private final AnswerResponseDto myAnswer;

    public QuestionResponseDto(final Question question, final long participantCount, final Answer answer) {
        this.id = question.getId();
        this.date = question.getPublishDate();
        this.title = question.getTitle();
        this.body = question.getBody();
        this.participantCount = participantCount;
        this.choices = question.getChoices().stream().map(ChoiceResponseDto::new).toList();
        this.myAnswer = answer == null ? null : new AnswerResponseDto(answer);
    }
}
