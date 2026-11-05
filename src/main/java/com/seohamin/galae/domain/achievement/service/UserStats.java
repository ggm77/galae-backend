package com.seohamin.galae.domain.achievement.service;

/**
 * 업적 판정과 프로필 숫자에 쓰는 유저 활동 통계 (docs/ACHIEVEMENTS.md 판정 기준)
 *
 * @param answers       답변 수 (선택하기를 누른 질문 수. 세계수의 선 수)
 * @param pastAnswers   지난 질문에 답한 수 (답한 날이 질문 날과 다름)
 * @param shares        그룹에 나눈 질문 수
 * @param comments      댓글 수
 * @param likes         공감 수
 * @param groupOwner    만든 그룹이 있는지
 * @param currentStreak 지금 이어지는 연속 일수
 * @param longestStreak 가장 길게 이어진 연속 일수
 */
public record UserStats(
        long answers,
        long pastAnswers,
        long shares,
        long comments,
        long likes,
        boolean groupOwner,
        int currentStreak,
        int longestStreak
) {}
