package com.seohamin.galae.domain.admin.ai.dto;

import lombok.Getter;

/**
 * 관리자 페이지에 보여 줄 AI 설정 (키는 내보내지 않는다)
 */
@Getter
public class AiConfigResponseDto {

    private final Boolean configured;
    private final String defaultModel;
    private final String promptVersion;

    public AiConfigResponseDto(final boolean configured, final String defaultModel, final String promptVersion) {
        this.configured = configured;
        this.defaultModel = defaultModel;
        this.promptVersion = promptVersion;
    }
}
