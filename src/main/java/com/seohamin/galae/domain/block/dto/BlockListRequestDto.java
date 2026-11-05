package com.seohamin.galae.domain.block.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * 차단 목록 전체 교체. 아이디(handle)로 지정한다
 * 이 목록에 없는 기존 차단은 풀린다. 빈 배열이면 모두 해제
 */
@Getter
public class BlockListRequestDto {

    @NotNull
    @Size(max = 1000)
    private List<@NotNull String> handles;
}
