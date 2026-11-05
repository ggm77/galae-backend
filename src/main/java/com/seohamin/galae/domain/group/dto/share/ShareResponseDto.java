package com.seohamin.galae.domain.group.dto.share;

import lombok.Getter;

import java.util.List;

/**
 * 나누기 상태
 * sentGroupIds: 지금 답(선택·이유)을 이미 보낸 그룹. 나누기 화면에서 체크된 채 풀 수 없고,
 * 완료·요약의 공유 표시도 이 그룹만 센다 (docs/APP_FLOW.md 5-8)
 */
@Getter
public class ShareResponseDto {

    private final String questionId;
    private final List<Long> sentGroupIds;
    //골라 두고 아직 보내지 않은 그룹. 초안이 없으면 null
    private final List<Long> shareDraft;

    public ShareResponseDto(final String questionId, final List<Long> sentGroupIds, final List<Long> shareDraft) {
        this.questionId = questionId;
        this.sentGroupIds = sentGroupIds;
        this.shareDraft = shareDraft;
    }
}
