package com.seohamin.galae.domain.admin.question.dto;

import com.seohamin.galae.domain.question.dto.ChoiceResponseDto;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.QuestionStatus;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자용 질문 (공개 전·초안 해설 포함)
 * 한 선택지가 80%를 넘으면 실패한 질문으로 보고 재검토한다 (docs/PLAN.md 4.3) - needsReview
 */
@Getter
public class AdminQuestionResponseDto {

    public static final int REVIEW_THRESHOLD_PCT = 80;

    private final String id;
    private final LocalDate date;
    private final String title;
    private final String body;
    private final QuestionStatus status;
    //관리자용 이미지 주소 (공개 전 질문도 볼 수 있음, 토큰 필요). 없으면 null
    private final String imageUrl;
    private final List<ChoiceResponseDto> choices;
    private final List<AdminPerspectiveResponseDto> perspectives;
    private final Long participantCount;
    //가장 많이 고른 선택지의 비율
    private final Integer maxChoicePct;
    private final Boolean needsReview;

    public AdminQuestionResponseDto(final Question question, final long participantCount, final int maxChoicePct) {
        this.id = question.getId();
        this.date = question.getPublishDate();
        this.title = question.getTitle();
        this.body = question.getBody();
        this.status = question.getStatus();
        this.imageUrl = question.hasImage()
                ? "/api/v1/admin/questions/" + question.getId() + "/image?v=" + question.getImageUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC)
                : null;
        this.choices = question.getChoices().stream().map(ChoiceResponseDto::new).toList();
        this.perspectives = question.getPerspectives().stream().map(AdminPerspectiveResponseDto::new).toList();
        this.participantCount = participantCount;
        this.maxChoicePct = maxChoicePct;
        this.needsReview = participantCount > 0 && maxChoicePct > REVIEW_THRESHOLD_PCT;
    }
}
