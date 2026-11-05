package com.seohamin.galae.domain.user.dto;

import com.seohamin.galae.global.validation.Patterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 프로필 편집: 사진(256px), 색, 이름(10자), 아이디, 소개(80자)
 * null인 필드는 바꾸지 않는다
 */
@Getter
public class UserRequestDto {

    @Pattern(regexp = Patterns.HANDLE, message = "영문 소문자, 숫자, 마침표, 밑줄로 20자 이내여야 합니다.")
    private String handle;

    @Size(min = 1, max = 10)
    private String name;

    @Size(max = 80)
    private String bio;

    @Pattern(regexp = Patterns.COLOR)
    private String color;

    //data:image/jpeg;base64,... 빈 문자열이면 사진 삭제
    private String photo;
}
