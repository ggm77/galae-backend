package com.seohamin.galae.domain.question.dto;

import lombok.Getter;

import java.util.List;

/**
 * 선택지 코멘트: 그 선택지를 고른 사람들의 이유
 * 전체 분포에서는 익명이라 작성자를 주지 않는다
 */
@Getter
public class OpinionListResponseDto {

    private final String questionId;
    private final Integer choiceId;
    //내가 이 선택지를 골랐고 이유를 남겼으면 맨 위에 보여 줄 내 이유. 아니면 null
    private final String myReason;
    private final List<String> opinions;
    private final Integer page;
    private final Boolean hasNext;

    public OpinionListResponseDto(
            final String questionId,
            final Integer choiceId,
            final String myReason,
            final List<String> opinions,
            final Integer page,
            final Boolean hasNext
    ) {
        this.questionId = questionId;
        this.choiceId = choiceId;
        this.myReason = myReason;
        this.opinions = opinions;
        this.page = page;
        this.hasNext = hasNext;
    }
}
