package com.seohamin.galae.domain.user.dto;

import com.seohamin.galae.domain.user.entity.User;
import lombok.Getter;

import java.time.ZoneOffset;

/**
 * 목록·메시지·댓글에 붙는 간단한 유저 정보
 */
@Getter
public class UserSimpleResponseDto {

    private final Long id;
    private final String handle;
    private final String name;
    private final String color;
    //사진이 없으면 null. 서버 기준 상대 경로
    private final String photoUrl;

    public UserSimpleResponseDto(final User user) {
        this.id = user.getId();
        this.handle = user.getHandle();
        this.name = user.getName();
        this.color = user.getColor();
        this.photoUrl = photoUrlOf(user);
    }

    public static String photoUrlOf(final User user) {
        if (!user.hasPhoto()) {
            return null;
        }
        //사진이 바뀌면 v가 바뀌어 브라우저 캐시를 새로 받는다
        return "/api/v1/users/" + user.getId() + "/photo?v=" + user.getPhotoUpdatedAt().toEpochSecond(ZoneOffset.UTC);
    }
}
