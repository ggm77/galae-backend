package com.seohamin.galae.domain.question.entity.perspective;

import com.seohamin.galae.domain.question.entity.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 확장하기의 관점별 해설 (docs/PLAN.md 7.5의 commentaries)
 * 성경·철학·오늘의 연구 등 관점을 label로 구분한다
 * 사람이 검수해 APPROVED가 된 것만 보인다
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "perspective",
        indexes = @Index(name = "idx_perspective_status", columnList = "status")
)
public class Perspective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    //관점 이름 (성경, 철학, 오늘의 연구 등)
    @Column(length = 20, nullable = false)
    private String label;

    @Column(length = 2000, nullable = false)
    private String body;

    //인용 구절 원문 (개역한글은 원문 그대로, docs/PLAN.md 7.1)
    @Column(length = 500, nullable = true)
    private String verse;

    //출처 (예: 에베소서 4:15)
    @Column(length = 100, nullable = true)
    private String ref;

    //관점 순서
    @Column(nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PerspectiveStatus status;

    //AI 생성일 때 모델과 프롬프트 버전 (docs/PLAN.md 7.5 - 비교 측정용)
    @Column(length = 50, nullable = true)
    private String model;

    @Column(length = 30, nullable = true)
    private String promptVersion;

    //검수한 관리자 ID
    @Column(nullable = true)
    private Long reviewedBy;

    @Column(nullable = true)
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Perspective(
            final String label,
            final String body,
            final String verse,
            final String ref,
            final Integer sortOrder,
            final PerspectiveStatus status,
            final String model,
            final String promptVersion
    ) {
        this.label = label;
        this.body = body;
        this.verse = verse;
        this.ref = ref;
        this.sortOrder = sortOrder;
        this.status = status;
        this.model = model;
        this.promptVersion = promptVersion;
    }

    public void updateQuestion(final Question question) {
        this.question = question;
    }

    public void updateContent(final String label, final String body, final String verse, final String ref, final Integer sortOrder) {
        this.label = label;
        this.body = body;
        this.verse = verse;
        this.ref = ref;
        this.sortOrder = sortOrder;
    }

    public void review(final PerspectiveStatus status, final Long reviewerId) {
        this.status = status;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }
}
