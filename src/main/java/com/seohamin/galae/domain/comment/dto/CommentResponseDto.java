package com.seohamin.galae.domain.comment.dto;

import com.seohamin.galae.domain.comment.entity.Comment;
import com.seohamin.galae.domain.user.dto.UserSimpleResponseDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 (아이디 표시, 공감 수, 답글)
 */
@Getter
public class CommentResponseDto {

    private final Long id;
    private final String topicId;
    private final Long parentId;
    private final UserSimpleResponseDto author;
    private final String text;
    private final Integer likeCount;
    private final Boolean liked;
    private final Boolean mine;
    private final LocalDateTime createdAt;
    //원 댓글일 때만 채운다. 답글이면 빈 목록
    private final List<CommentResponseDto> replies;

    public CommentResponseDto(
            final Comment comment,
            final Long viewerId,
            final boolean liked,
            final List<CommentResponseDto> replies
    ) {
        this.id = comment.getId();
        this.topicId = comment.getTopic().getId();
        this.parentId = comment.getParent() == null ? null : comment.getParent().getId();
        this.author = new UserSimpleResponseDto(comment.getAuthor());
        this.text = comment.getText();
        this.likeCount = comment.getLikeCount();
        this.liked = liked;
        this.mine = comment.getAuthor().getId().equals(viewerId);
        this.createdAt = comment.getCreatedAt();
        this.replies = replies;
    }
}
