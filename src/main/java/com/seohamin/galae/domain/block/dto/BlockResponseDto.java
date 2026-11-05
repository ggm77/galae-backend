package com.seohamin.galae.domain.block.dto;

import com.seohamin.galae.domain.block.entity.UserBlock;
import com.seohamin.galae.domain.user.dto.UserSimpleResponseDto;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 차단한 사용자 한 명
 */
@Getter
public class BlockResponseDto {

    private final UserSimpleResponseDto user;
    private final LocalDateTime blockedAt;

    public BlockResponseDto(final UserBlock block) {
        this.user = new UserSimpleResponseDto(block.getBlocked());
        this.blockedAt = block.getCreatedAt();
    }
}
