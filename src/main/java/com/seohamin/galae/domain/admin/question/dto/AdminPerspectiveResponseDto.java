package com.seohamin.galae.domain.admin.question.dto;

import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import com.seohamin.galae.domain.question.entity.perspective.PerspectiveStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminPerspectiveResponseDto {

    private final Long id;
    private final String questionId;
    private final String label;
    private final String body;
    private final String verse;
    private final String ref;
    private final String openQuestion;
    private final Integer sortOrder;
    private final PerspectiveStatus status;
    private final String model;
    private final String promptVersion;
    //AI 생성 근거·검증 결과 (JSON 문자열). 사람이 쓴 해설은 null
    private final String generationDetail;
    private final Long reviewedBy;
    private final LocalDateTime reviewedAt;
    private final LocalDateTime createdAt;

    public AdminPerspectiveResponseDto(final Perspective perspective) {
        this.id = perspective.getId();
        this.questionId = perspective.getQuestion().getId();
        this.label = perspective.getLabel();
        this.body = perspective.getBody();
        this.verse = perspective.getVerse();
        this.ref = perspective.getRef();
        this.openQuestion = perspective.getOpenQuestion();
        this.sortOrder = perspective.getSortOrder();
        this.status = perspective.getStatus();
        this.model = perspective.getModel();
        this.promptVersion = perspective.getPromptVersion();
        this.generationDetail = perspective.getGenerationDetail();
        this.reviewedBy = perspective.getReviewedBy();
        this.reviewedAt = perspective.getReviewedAt();
        this.createdAt = perspective.getCreatedAt();
    }
}
