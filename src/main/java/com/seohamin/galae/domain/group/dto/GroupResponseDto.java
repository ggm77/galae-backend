package com.seohamin.galae.domain.group.dto;

import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.group.entity.member.GroupMember;
import com.seohamin.galae.domain.user.dto.UserSimpleResponseDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 그룹 상세 (멤버 화면)
 */
@Getter
public class GroupResponseDto {

    private final Long id;
    private final String name;
    private final String description;
    private final String color;
    private final String photoUrl;
    private final String inviteCode;
    private final Long ownerId;
    //내가 그룹장인지
    private final Boolean owner;
    private final List<Member> members;
    private final LocalDateTime createdAt;

    public GroupResponseDto(final GalaeGroup group, final List<GroupMember> members, final Long viewerId) {
        this.id = group.getId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.color = group.getColor();
        this.photoUrl = photoUrlOf(group);
        this.inviteCode = group.getInviteCode();
        this.ownerId = group.getOwner().getId();
        this.owner = group.isOwnedBy(viewerId);
        this.members = members.stream().map(m -> new Member(m, group)).toList();
        this.createdAt = group.getCreatedAt();
    }

    public static String photoUrlOf(final GalaeGroup group) {
        if (!group.hasPhoto()) {
            return null;
        }
        return "/api/v1/groups/" + group.getId() + "/photo?v=" + group.getPhotoUpdatedAt().toEpochSecond(ZoneOffset.UTC);
    }

    @Getter
    public static class Member {
        private final UserSimpleResponseDto user;
        private final Boolean owner;
        private final LocalDateTime joinedAt;

        public Member(final GroupMember member, final GalaeGroup group) {
            this.user = new UserSimpleResponseDto(member.getUser());
            this.owner = group.isOwnedBy(member.getUser().getId());
            this.joinedAt = member.getJoinedAt();
        }
    }
}
