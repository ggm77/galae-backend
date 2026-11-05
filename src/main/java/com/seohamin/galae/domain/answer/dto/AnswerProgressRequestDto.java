package com.seohamin.galae.domain.answer.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

/**
 * 진행 상태 저장. null인 필드는 바꾸지 않는다
 * 미완료 질문은 마지막으로 보던 단계에서 이어지고, 공유 초안은 기기를 바꿔도 남는다
 */
@Getter
public class AnswerProgressRequestDto {

    @Pattern(regexp = "^(answer|result|reading|done|share|summary)$")
    private String lastStep;

    //나누기에서 골라 둔 그룹 ID (가입한 그룹만). 빈 배열이면 '나만 보기' 초안
    private List<Long> shareDraft;
}
