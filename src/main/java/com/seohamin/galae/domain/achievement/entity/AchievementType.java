package com.seohamin.galae.domain.achievement.entity;

import com.seohamin.galae.domain.achievement.service.UserStats;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 업적 15개 (docs/ACHIEVEMENTS.md, 프론트 src/achievements.js)
 * 열매 하나가 업적 하나다. code는 프론트의 업적 id와 같다
 */
@Getter
public enum AchievementType {

    //처음 해 본 순간
    FIRST("first", "첫 가지", "처음으로 답했어요", s -> s.answers() >= 1),
    PAST("past", "여행자", "지난 질문에 처음 답했어요", s -> s.pastAnswers() >= 1),
    SHARE1("share1", "길동무", "처음으로 그룹에 생각을 나눴어요", s -> s.shares() >= 1),
    COMMENT1("comment1", "말동무", "처음으로 댓글을 남겼어요", s -> s.comments() >= 1),
    GROUP("group", "개척자", "처음으로 그룹을 만들었어요", UserStats::groupOwner),

    //꾸준함
    STREAK7("streak7", "뿌리", "7일 연속으로 답했어요", s -> s.longestStreak() >= 7),
    STREAK30("streak30", "수행자", "30일 연속으로 답했어요", s -> s.longestStreak() >= 30),
    STREAK100("streak100", "오래된 나무", "100일 연속으로 답했어요", s -> s.longestStreak() >= 100),
    ANSWER10("answer10", "탐험가", "10번째 답을 남겼어요", s -> s.answers() >= 10),
    ANSWER30("answer30", "사색가", "30번째 답을 남겼어요", s -> s.answers() >= 30),
    ANSWER100("answer100", "현자", "100번째 답을 남겼어요", s -> s.answers() >= 100),
    ANSWER200("answer200", "세계수", "200번째 답을 남겼어요", s -> s.answers() >= 200),
    SHARE10("share10", "동반자", "그룹에 10번 생각을 나눴어요", s -> s.shares() >= 10),
    COMMENT100("comment100", "논객", "댓글을 100개 남겼어요", s -> s.comments() >= 100),
    LIKE100("like100", "경청자", "다른 생각에 100번 공감했어요", s -> s.likes() >= 100);

    private final String code;
    private final String title;
    private final String description;
    private final Predicate<UserStats> condition;

    AchievementType(final String code, final String title, final String description, final Predicate<UserStats> condition) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.condition = condition;
    }

    public boolean isAchieved(final UserStats stats) {
        return condition.test(stats);
    }

    public static Optional<AchievementType> fromCode(final String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }
}
