package com.seohamin.galae.domain.answer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 선택하기: 선택은 필수, 이유는 선택 입력(500자)
 */
@Getter
public class AnswerRequestDto {

    @NotNull
    @Min(0)
    private Integer choiceId;

    @Size(max = 500)
    private String reason;
}
