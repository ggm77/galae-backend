package com.seohamin.galae.domain.admin.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 관점별 해설 등록·수정 (관리자 또는 AI 파이프라인 결과 적재)
 * 등록·수정하면 DRAFT가 되고, 검수에서 승인해야 앱에 보인다
 */
@Getter
public class AdminPerspectiveRequestDto {

    @NotBlank
    @Size(max = 20)
    private String label;

    @NotBlank
    @Size(max = 2000)
    private String body;

    @Size(max = 500)
    private String verse;

    @Size(max = 100)
    private String ref;

    private Integer sortOrder;

    @Size(max = 50)
    private String model;

    @Size(max = 30)
    private String promptVersion;
}
