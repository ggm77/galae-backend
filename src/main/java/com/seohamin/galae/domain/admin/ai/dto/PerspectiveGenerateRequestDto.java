package com.seohamin.galae.domain.admin.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * AI 해설 생성 요청 (관리자)
 * 필수 가치는 모델에게 고르게 하지 않고 사람이 정해 넣는다 (docs/PLAN.md 5.5)
 * 인용은 여기 넣은 출처 목록 안에서만 허용한다 (docs/PLAN.md 4.2, 5.2)
 */
@Getter
public class PerspectiveGenerateRequestDto {

    //관점 (성경, 철학, 불교, 오늘의 연구 등)
    @NotBlank
    @Size(max = 20)
    private String label;

    //필수 가치 태그 2~5개 (예: 정직, 회복)
    @NotNull
    @Size(min = 2, max = 5)
    private List<@NotBlank @Size(max = 20) String> valueTags;

    //인용할 수 있는 출처 (없으면 특정 문헌 인용 없이 일반적 입장만 서술)
    @Size(max = 12)
    @Valid
    private List<SourceItem> sources;

    //모델 ID. 없으면 기본 모델 (예: anthropic/claude-sonnet-5.5)
    @Pattern(regexp = "^[a-z0-9._-]+/[a-z0-9._-]+$")
    private String model;

    @Getter
    public static class SourceItem {

        //주소 (예: 에베소서 4:15)
        @NotBlank
        @Size(max = 100)
        private String ref;

        //원문 그대로 (개역한글 등). 서버가 이 글자를 그대로 인용 구절로 붙인다
        @NotBlank
        @Size(max = 500)
        private String text;

        //맥락 노트 (docs/PLAN.md 7.4). 모델이 표면적으로 갖다 붙이지 않게 돕는다
        @Size(max = 300)
        private String note;
    }
}
