package com.seohamin.galae.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 회원 탈퇴: 실수를 막기 위해 비밀번호를 한 번 더 받는다
 */
@Getter
public class WithdrawRequestDto {

    @NotNull
    private String password;
}
