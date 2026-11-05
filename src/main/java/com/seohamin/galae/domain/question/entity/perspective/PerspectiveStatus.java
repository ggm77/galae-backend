package com.seohamin.galae.domain.question.entity.perspective;

/**
 * 해설 검수 상태 (docs/PLAN.md 5.3, 5.7)
 * APPROVED만 앱에 노출된다
 */
public enum PerspectiveStatus {
    DRAFT,
    APPROVED,
    REJECTED,
    //자동 검증 2회 실패 (AI 파이프라인용)
    FAILED
}
