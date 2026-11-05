package com.seohamin.galae.domain.question.dto;

import com.seohamin.galae.domain.question.entity.choice.Choice;
import lombok.Getter;

/**
 * 선택지. 비율은 내 답을 남긴 뒤 결과 API에서만 준다
 */
@Getter
public class ChoiceResponseDto {

    //프론트의 선택지 id (0, 1, 2)
    private final Integer id;
    private final String text;
    private final String shortText;

    public ChoiceResponseDto(final Choice choice) {
        this.id = choice.getChoiceIndex();
        this.text = choice.getText();
        this.shortText = choice.getShortText();
    }
}
