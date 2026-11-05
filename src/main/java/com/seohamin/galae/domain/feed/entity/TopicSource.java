package com.seohamin.galae.domain.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주제의 출처 (원전이나 참고 자료)
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicSource {

    @Column(length = 200, nullable = false)
    private String label;

    @Column(length = 500, nullable = true)
    private String url;

    public TopicSource(final String label, final String url) {
        this.label = label;
        this.url = url;
    }
}
