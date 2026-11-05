package com.seohamin.galae.domain.answer.dto;

import com.seohamin.galae.domain.answer.entity.Answer;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 나의 답변
 */
@Getter
public class AnswerResponseDto {

    private final String questionId;
    private final Integer choiceId;
    private final String choiceShortText;
    private final String reason;
    private final Boolean completed;
    private final LocalDate answeredOn;
    private final String lastStep;
    //나누기에서 골라 둔 그룹 ID. 초안이 없으면 null
    private final List<Long> shareDraft;
    private final LocalDateTime updatedAt;

    public AnswerResponseDto(final Answer answer) {
        this.questionId = answer.getQuestion().getId();
        this.choiceId = answer.getChoice().getChoiceIndex();
        this.choiceShortText = answer.getChoice().getShortText();
        this.reason = answer.getReason();
        this.completed = answer.getCompleted();
        this.answeredOn = answer.getAnsweredOn();
        this.lastStep = answer.getLastStep();
        this.shareDraft = answer.getHasShareDraft() ? List.copyOf(answer.getShareDraft()) : null;
        this.updatedAt = answer.getUpdatedAt();
    }
}
