package com.seohamin.galae.domain.user.dto;

import com.seohamin.galae.domain.achievement.dto.AchievementResponseDto;
import com.seohamin.galae.domain.achievement.service.UserStats;
import com.seohamin.galae.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 프로필 (나 탭, 다른 사람 프로필)
 * 이름, 숫자(선택·연속·댓글), 소개, 열매, 나무(답변 수 = 선 수)
 * 가입일·권한은 내 프로필에서만 준다
 */
@Getter
public class UserProfileResponseDto {

    private final Long id;
    private final String handle;
    private final String name;
    private final String bio;
    private final String color;
    private final String photoUrl;
    private final Stats stats;
    private final List<AchievementResponseDto> achievements;
    //내 프로필에서만 채운다
    private final LocalDate joinedOn;
    private final String role;

    public UserProfileResponseDto(
            final User user,
            final UserStats stats,
            final List<AchievementResponseDto> achievements,
            final boolean mine
    ) {
        this.id = user.getId();
        this.handle = user.getHandle();
        this.name = user.getName();
        this.bio = user.getBio();
        this.color = user.getColor();
        this.photoUrl = UserSimpleResponseDto.photoUrlOf(user);
        this.stats = new Stats(stats);
        this.achievements = achievements;
        this.joinedOn = mine ? user.getJoinedOn() : null;
        this.role = mine ? user.getRole().name() : null;
    }

    @Getter
    public static class Stats {
        //선택 수 (세계수의 선 수)
        private final Long answerCount;
        private final Integer currentStreak;
        private final Integer longestStreak;
        private final Long commentCount;

        public Stats(final UserStats stats) {
            this.answerCount = stats.answers();
            this.currentStreak = stats.currentStreak();
            this.longestStreak = stats.longestStreak();
            this.commentCount = stats.comments();
        }
    }
}
