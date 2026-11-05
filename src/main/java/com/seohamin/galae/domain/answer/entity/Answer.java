package com.seohamin.galae.domain.answer.entity;

import com.seohamin.galae.domain.question.entity.Question;
import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 질문에 남긴 나의 선택과 이유 (docs/PLAN.md 7.5의 responses + opinions)
 * 선택하기를 누른 순간 만들어진다. 유저·질문당 하나
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "answer",
        uniqueConstraints = @UniqueConstraint(name = "uk_answer_user_question", columnNames = {"user_id", "question_id"}),
        indexes = {
                @Index(name = "idx_answer_question_choice", columnList = "question_id, choice_id"),
                @Index(name = "idx_answer_user_answered_on", columnList = "user_id, answered_on")
        }
)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "choice_id", nullable = false)
    private Choice choice;

    //이유 (선택 입력, 500자). 없으면 빈 문자열
    @Column(length = 500, nullable = false)
    private String reason;

    //확장하기에서 다음을 눌러 끝냈는지
    @Column(nullable = false)
    private Boolean completed;

    //처음 답한 날. 다시 하기로 답을 바꿔도 그대로다
    @Column(name = "answered_on", nullable = false)
    private LocalDate answeredOn;

    //마지막으로 보던 단계 (answer, result, reading, done, share, summary)
    @Column(length = 20, nullable = true)
    private String lastStep;

    //나누기에서 골라 둔 그룹 (보내기 전 초안). null이면 초안 없음
    @ElementCollection
    @CollectionTable(name = "answer_share_draft", joinColumns = @JoinColumn(name = "answer_id"))
    @Column(name = "group_id")
    private Set<Long> shareDraft = new LinkedHashSet<>();

    //초안이 있는지 (빈 목록과 초안 없음을 구분)
    @Column(nullable = false)
    private Boolean hasShareDraft;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Answer(
            final User user,
            final Question question,
            final Choice choice,
            final String reason,
            final LocalDate answeredOn
    ) {
        this.user = user;
        this.question = question;
        this.choice = choice;
        this.reason = reason;
        this.answeredOn = answeredOn;
        this.completed = false;
        this.hasShareDraft = false;
    }

    /**
     * 다시 하기로 선택·이유를 바꾼다.
     * 처음 답한 날·완료 같은 나머지 기록은 그대로이고,
     * 바뀐 답은 아직 아무 그룹에도 보내지 않은 상태라서 공유 초안을 비운다 (docs/APP_FLOW.md 5-5)
     * @return 바뀌었으면 true
     */
    public boolean resubmit(final Choice choice, final String reason) {
        if (this.choice.getId().equals(choice.getId()) && this.reason.equals(reason)) {
            return false;
        }
        this.choice = choice;
        this.reason = reason;
        clearShareDraft();
        return true;
    }

    public void complete() {
        this.completed = true;
    }

    public void updateLastStep(final String lastStep) {
        this.lastStep = lastStep;
    }

    public void updateShareDraft(final Set<Long> groupIds) {
        this.shareDraft.clear();
        this.shareDraft.addAll(groupIds);
        this.hasShareDraft = true;
    }

    public void clearShareDraft() {
        this.shareDraft.clear();
        this.hasShareDraft = false;
    }

    public void removeFromShareDraft(final Long groupId) {
        this.shareDraft.remove(groupId);
    }
}
