package com.seohamin.galae.domain.comment.entity;

import com.seohamin.galae.domain.feed.entity.Topic;
import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 생각 탭 주제의 댓글과 답글
 * 답글은 한 단계만 둔다 (답글에 답하면 원 댓글의 답글이 된다)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "comment",
        indexes = {
                @Index(name = "idx_comment_topic", columnList = "topic_id, created_at"),
                @Index(name = "idx_comment_author", columnList = "author_id, created_at")
        }
)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    //답글이면 원 댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Comment parent;

    @Column(length = 500, nullable = false)
    private String text;

    //공감 수 (조회 성능을 위해 비정규화)
    @Column(nullable = false)
    private Integer likeCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Comment(final Topic topic, final User author, final Comment parent, final String text) {
        this.topic = topic;
        this.author = author;
        this.parent = parent;
        this.text = text;
        this.likeCount = 0;
    }
}
