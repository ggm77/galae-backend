package com.seohamin.galae.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CommentRequestDto {

    @NotBlank
    @Size(max = 500)
    private String text;

    //답글이면 원 댓글 ID
    private Long parentId;
}
