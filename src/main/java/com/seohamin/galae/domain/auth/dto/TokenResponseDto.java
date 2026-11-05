package com.seohamin.galae.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponseDto {

    private final Long userId;
    private final String tokenType;
    private final String accessToken;
    //액세스 토큰 만료까지 남은 초
    private final Long exprTime;
    private final String refreshToken;
}
