package com.seohamin.galae.domain.admin.question.dto;

import com.seohamin.galae.domain.question.entity.QuestionStatus;
import com.seohamin.galae.global.validation.Create;
import com.seohamin.galae.global.validation.Patterns;
import com.seohamin.galae.global.validation.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 질문 등록·수정 (관리자)
 * 수정할 때는 null인 필드를 바꾸지 않는다
 * 선택지는 3개를 넘지 않는다 (docs/PLAN.md 4.3)
 */
@Getter
public class AdminQuestionRequestDto {

    @NotNull(groups = Create.class)
    @Pattern(regexp = Patterns.QUESTION_ID, groups = Create.class)
    private String id;

    @NotNull(groups = Create.class)
    private LocalDate date;

    @NotBlank(groups = Create.class)
    @Size(max = 100, groups = {Create.class, Update.class})
    private String title;

    @NotBlank(groups = Create.class)
    @Size(max = 1000, groups = {Create.class, Update.class})
    private String body;

    //없으면 DRAFT로 등록
    private QuestionStatus status;

    //질문 이미지 data URL (data:image/jpeg|png|webp;base64,...), 1MB 이하
    //null이면 그대로, 빈 문자열이면 이미지 삭제
    private String image;

    @NotNull(groups = Create.class)
    @Size(min = 2, max = 3, groups = {Create.class, Update.class})
    @Valid
    private List<ChoiceItem> choices;

    @Getter
    public static class ChoiceItem {

        @NotBlank(groups = {Create.class, Update.class})
        @Size(max = 200, groups = {Create.class, Update.class})
        private String text;

        @NotBlank(groups = {Create.class, Update.class})
        @Size(max = 50, groups = {Create.class, Update.class})
        private String shortText;
    }
}
