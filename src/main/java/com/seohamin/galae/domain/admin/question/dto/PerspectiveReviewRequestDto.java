package com.seohamin.galae.domain.admin.question.dto;

import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 검수: 승인 또는 반려
 */
@Getter
public class PerspectiveReviewRequestDto {

    @NotNull
    private PerspectiveStatus status;
}
