package com.seohamin.galae.domain.achievement.dto;

import com.seohamin.galae.domain.achievement.entity.UserAchievement;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 맺은 열매. 고리를 누르면 이름, 날짜, 설명 한 줄이 나온다
 */
@Getter
public class AchievementResponseDto {

    //프론트의 업적 id (first, streak7 등)
    private final String id;
    private final String name;
    private final String description;
    private final LocalDate achievedOn;

    public AchievementResponseDto(final UserAchievement achievement) {
        this.id = achievement.getType().getCode();
        this.name = achievement.getType().getTitle();
        this.description = achievement.getType().getDescription();
        this.achievedOn = achievement.getAchievedOn();
    }
}
