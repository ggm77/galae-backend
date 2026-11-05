package com.seohamin.galae.domain.feed.dto;

import com.seohamin.galae.domain.feed.entity.Topic;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 생각 탭의 한 주제 (한 화면에 제목, 글, 출처)
 */
@Getter
public class TopicResponseDto {

    private final String id;
    private final String category;
    private final String title;
    private final List<String> cards;
    private final List<Source> sources;
    private final LocalDate checkedOn;
    private final Long commentCount;

    public TopicResponseDto(final Topic topic, final long commentCount) {
        this.id = topic.getId();
        this.category = topic.getCategory();
        this.title = topic.getTitle();
        this.cards = List.copyOf(topic.getCards());
        this.sources = topic.getSources().stream().map(s -> new Source(s.getLabel(), s.getUrl())).toList();
        this.checkedOn = topic.getCheckedOn();
        this.commentCount = commentCount;
    }

    @Getter
    public static class Source {
        private final String label;
        private final String url;

        public Source(final String label, final String url) {
            this.label = label;
            this.url = url;
        }
    }
}
