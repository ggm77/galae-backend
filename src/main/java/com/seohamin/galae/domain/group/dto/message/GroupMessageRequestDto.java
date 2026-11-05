package com.seohamin.galae.domain.group.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class GroupMessageRequestDto {

    @NotBlank
    @Size(max = 500)
    private String text;
}
