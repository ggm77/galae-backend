package com.seohamin.galae.domain.group.dto.share;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * 나누기: 고른 그룹에 지금 답을 보낸다. 빈 배열이면 '나만 보기'
 */
@Getter
public class ShareRequestDto {

    @NotNull
    @Size(max = 100)
    private List<@NotNull Long> groupIds;
}
