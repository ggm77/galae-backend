package com.seohamin.galae.domain.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoginRequestDto {

    @NotNull
    private String handle;

    @NotNull
    private String password;
}
