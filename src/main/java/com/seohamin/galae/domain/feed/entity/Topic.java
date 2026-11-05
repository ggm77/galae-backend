package com.seohamin.galae.domain.feed.entity;

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

/**
 * 생각 탭의 주제 (프론트 content/paradoxes.json)
 * 짧은 글 4~5장(cards)과 출처로 이루어진다
 * 검증을 마친 날(checkedOn)이 있는 주제만 노출한다 (docs/VERIFICATION.md)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "topic")
public class Topic {

    @Id
    @Column(length = 20)
    private String id;

    //동일성, 윤리, 논리 등
    @Column(length = 20, nullable = false)
    private String category;

    @Column(length = 100, nullable = false)
    private String title;

    @ElementCollection
    @CollectionTable(name = "topic_card", joinColumns = @JoinColumn(name = "topic_id"))
    @OrderColumn(name = "card_index")
    @Column(name = "text", length = 500, nullable = false)
    private List<String> cards = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "topic_source", joinColumns = @JoinColumn(name = "topic_id"))
    @OrderColumn(name = "source_index")
    private List<TopicSource> sources = new ArrayList<>();

    //검증을 마친 날. null이면 보류 (배포 환경에서는 노출하지 않음)
    @Column(nullable = true)
    private LocalDate checkedOn;

    //주제 이미지가 바뀐 시각. null이면 이미지 없음
    //이미지 바이트는 피드 조회 때 함께 읽히지 않도록 topic_image 테이블에 따로 둔다 (TopicImage)
    @Column(nullable = true)
    private LocalDateTime imageUpdatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Topic(
            final String id,
            final String category,
            final String title,
            final List<String> cards,
            final List<TopicSource> sources,
            final LocalDate checkedOn
    ) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.cards.addAll(cards);
        this.sources.addAll(sources);
        this.checkedOn = checkedOn;
    }

    public void updateCheckedOn(final LocalDate checkedOn) {
        this.checkedOn = checkedOn;
    }

    public void updateCategory(final String category) {
        this.category = category;
    }

    public void updateTitle(final String title) {
        this.title = title;
    }

    public void replaceCards(final List<String> cards) {
        this.cards.clear();
        this.cards.addAll(cards);
    }

    public void replaceSources(final List<TopicSource> sources) {
        this.sources.clear();
        this.sources.addAll(sources);
    }

    public boolean hasImage() {
        return imageUpdatedAt != null;
    }

    //이미지를 넣거나 바꾸면 시각을 남기고, 빼면 null
    public void markImageUpdated(final boolean present) {
        this.imageUpdatedAt = present ? LocalDateTime.now() : null;
    }
}
