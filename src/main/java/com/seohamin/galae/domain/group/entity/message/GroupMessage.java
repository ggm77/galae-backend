package com.seohamin.galae.domain.group.entity.message;

import com.seohamin.galae.domain.group.entity.GalaeGroup;
import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 그룹 대화의 메시지 (docs/APP_FLOW.md 7)
 * 공유 메시지는 { groupId, questionId, sel, text } - choice가 있는 메시지다
 * 일반 메시지는 choice가 null이다
 * 보낸 메시지는 수정하거나 거둘 수 없다 (docs/APP_FLOW.md 5-8)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "group_message",
        indexes = {
                @Index(name = "idx_group_message_group_question", columnList = "group_id, question_id, created_at"),
                @Index(name = "idx_group_message_author_question", columnList = "author_id, question_id")
        }
)
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GalaeGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    //공유 메시지면 보낸 순간의 선택. 일반 메시지는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id", nullable = true)
    private Choice choice;

    //일반 메시지는 내용, 공유 메시지는 보낸 순간의 이유 (비어 있어도 됨)
    @Column(length = 500, nullable = false)
    private String text;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public GroupMessage(
            final GalaeGroup group,
            final Question question,
            final User author,
            final Choice choice,
            final String text
    ) {
        this.group = group;
        this.question = question;
        this.author = author;
        this.choice = choice;
        this.text = text;
    }

    public boolean isShare() {
        return choice != null;
    }
}
