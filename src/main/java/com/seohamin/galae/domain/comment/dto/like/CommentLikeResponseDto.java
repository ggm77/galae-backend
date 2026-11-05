package com.seohamin.galae.domain.comment.dto.like;

import lombok.Getter;

@Getter
public class CommentLikeResponseDto {

    private final Long commentId;
    private final Boolean liked;
    private final Integer likeCount;

    public CommentLikeResponseDto(final Long commentId, final boolean liked, final int likeCount) {
        this.commentId = commentId;
        this.liked = liked;
        this.likeCount = likeCount;
    }
}
