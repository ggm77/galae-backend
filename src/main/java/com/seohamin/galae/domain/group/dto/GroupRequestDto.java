package com.seohamin.galae.domain.group.dto;

import com.seohamin.galae.global.validation.Create;
import com.seohamin.galae.global.validation.Patterns;
import com.seohamin.galae.global.validation.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 그룹 만들기·설정
 * 수정할 때는 null인 필드를 바꾸지 않는다
 */
@Getter
public class GroupRequestDto {

    @NotBlank(groups = Create.class)
    @Size(max = 20, groups = {Create.class, Update.class})
    private String name;

    @Size(max = 100, groups = {Create.class, Update.class})
    private String description;

    @NotNull(groups = Create.class)
    @Pattern(regexp = Patterns.COLOR, groups = {Create.class, Update.class})
    private String color;

    //data:image/jpeg;base64,... 빈 문자열이면 사진 삭제
    private String photo;
}
