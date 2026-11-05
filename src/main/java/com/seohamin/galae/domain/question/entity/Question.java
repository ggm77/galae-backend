package com.seohamin.galae.domain.question.entity;

import com.seohamin.galae.domain.question.entity.choice.Choice;
import com.seohamin.galae.domain.question.entity.perspective.Perspective;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 하루 한 질문 (오늘의 갈래)
 * ID는 프론트 경로에 그대로 쓰이는 문자열이다 (예: q037)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "question",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_publish_date", columnNames = "publish_date"),
        indexes = @Index(name = "idx_question_status_date", columnList = "status, publish_date")
)
public class Question {

    @Id
    @Column(length = 20)
    private String id;

    //공개일. 하루에 질문 하나
    @Column(name = "publish_date", nullable = false)
    private LocalDate publishDate;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 1000, nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private QuestionStatus status;

    //선택지 (3개 이하, docs/PLAN.md 4.3)
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("choiceIndex ASC")
    private List<Choice> choices = new ArrayList<>();

    //확장하기의 관점별 해설
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<Perspective> perspectives = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Question(
            final String id,
            final LocalDate publishDate,
            final String title,
            final String body,
            final QuestionStatus status
    ) {
        this.id = id;
        this.publishDate = publishDate;
        this.title = title;
        this.body = body;
        this.status = status;
    }

    public Optional<Choice> findChoice(final int choiceIndex) {
        return choices.stream().filter(c -> c.getChoiceIndex() == choiceIndex).findFirst();
    }

    public void addChoice(final Choice choice) {
        choice.updateQuestion(this);
        choices.add(choice);
    }

    public void addPerspective(final Perspective perspective) {
        perspective.updateQuestion(this);
        perspectives.add(perspective);
    }

    public void updatePublishDate(final LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public void updateTitle(final String title) {
        this.title = title;
    }

    public void updateBody(final String body) {
        this.body = body;
    }

    public void updateStatus(final QuestionStatus status) {
        this.status = status;
    }
}
