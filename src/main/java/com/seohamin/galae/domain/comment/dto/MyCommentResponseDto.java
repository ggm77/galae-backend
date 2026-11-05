package com.seohamin.galae.domain.comment.dto;

import com.seohamin.galae.domain.comment.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 내 댓글 목록 (누르면 그 주제로)
 */
@Getter
public class MyCommentResponseDto {

    private final Long id;
    private final String topicId;
    private final String topicTitle;
    private final Long parentId;
    private final String text;
    private final Integer likeCount;
    private final LocalDateTime createdAt;

    public MyCommentResponseDto(final Comment comment) {
        this.id = comment.getId();
        this.topicId = comment.getTopic().getId();
        this.topicTitle = comment.getTopic().getTitle();
        this.parentId = comment.getParent() == null ? null : comment.getParent().getId();
        this.text = comment.getText();
        this.likeCount = comment.getLikeCount();
        this.createdAt = comment.getCreatedAt();
    }
}
