package com.seohamin.galae.domain.question.entity;

/**
 * 질문 공개 상태
 * PUBLISHED이고 공개일이 오늘 이전인 질문만 사용자에게 보인다
 */
public enum QuestionStatus {
    DRAFT,
    PUBLISHED
}
