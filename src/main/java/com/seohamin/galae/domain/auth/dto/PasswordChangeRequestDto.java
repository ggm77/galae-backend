package com.seohamin.galae.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PasswordChangeRequestDto {

    @NotNull
    private String currentPassword;

    @NotNull
    @Size(min = 8, max = 72, message = "8자 이상이어야 합니다.")
    private String newPassword;
}
