package com.seohamin.galae.domain.comment.entity.like;

import com.seohamin.galae.domain.comment.entity.Comment;
import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 댓글 공감 (유저·댓글당 하나)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "comment_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_like", columnNames = {"comment_id", "user_id"}),
        indexes = @Index(name = "idx_comment_like_user", columnList = "user_id")
)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public CommentLike(final Comment comment, final User user) {
        this.comment = comment;
        this.user = user;
    }
}
