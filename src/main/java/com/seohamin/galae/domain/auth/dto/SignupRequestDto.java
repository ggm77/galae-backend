package com.seohamin.galae.domain.auth.dto;

import com.seohamin.galae.global.validation.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 가입 화면: 아이디(영문 소문자·숫자·.·_, 20자), 비밀번호(8자 이상), 이름(10자)
 */
@Getter
public class SignupRequestDto {

    @NotNull
    @Pattern(regexp = Patterns.HANDLE, message = "영문 소문자, 숫자, 마침표, 밑줄로 20자 이내여야 합니다.")
    private String handle;

    @NotNull
    @Size(min = 8, max = 72, message = "8자 이상이어야 합니다.")
    private String password;

    @NotBlank
    @Size(max = 10)
    private String name;
}
