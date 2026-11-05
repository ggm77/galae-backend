package com.seohamin.galae.domain.group.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GroupJoinRequestDto {

    //6자리 초대 코드 (대소문자·앞뒤 공백 무시)
    @NotBlank
    private String code;
}
