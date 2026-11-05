package com.seohamin.galae.domain.group.dto;

import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.user.dto.UserSimpleResponseDto;
import lombok.Getter;

import java.util.List;

/**
 * 가입한 그룹 목록의 한 줄 (그룹 탭, 오늘 탭의 그룹 카드)
 */
@Getter
public class GroupSummaryResponseDto {

    private final Long id;
    private final String name;
    private final String description;
    private final String color;
    private final String photoUrl;
    private final Long memberCount;
    private final Boolean owner;
    //오늘의 질문을 이 그룹에 나눈 멤버
    private final List<UserSimpleResponseDto> todaySharedMembers;

    public GroupSummaryResponseDto(
            final GalaeGroup group,
            final long memberCount,
            final boolean owner,
            final List<UserSimpleResponseDto> todaySharedMembers
    ) {
        this.id = group.getId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.color = group.getColor();
        this.photoUrl = GroupResponseDto.photoUrlOf(group);
        this.memberCount = memberCount;
        this.owner = owner;
        this.todaySharedMembers = todaySharedMembers;
    }
}
