package com.seohamin.galae.domain.feed.dto;

import com.seohamin.galae.domain.feed.entity.Topic;
import lombok.Getter;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 생각 탭의 한 주제 (한 화면에 제목, 글, 출처, 이미지)
 */
@Getter
public class TopicResponseDto {

    private final String id;
    private final String category;
    private final String title;
    private final List<String> cards;
    private final List<Source> sources;
    private final LocalDate checkedOn;
    //주제 이미지. 없으면 null. 서버 기준 상대 경로
    private final String imageUrl;
    private final Long commentCount;

    /**
     * 사용자용 (이미지는 토큰 없이 받는 공개 경로)
     */
    public TopicResponseDto(final Topic topic, final long commentCount) {
        this(topic, commentCount, "/api/v1/feed/topics/");
    }

    /**
     * @param imageBasePath 이미지 경로 앞부분. 관리자 화면은 공개 전 주제도 보려고 /api/v1/admin/topics/를 쓴다
     */
    public TopicResponseDto(final Topic topic, final long commentCount, final String imageBasePath) {
        this.id = topic.getId();
        this.category = topic.getCategory();
        this.title = topic.getTitle();
        this.cards = List.copyOf(topic.getCards());
        this.sources = topic.getSources().stream().map(s -> new Source(s.getLabel(), s.getUrl())).toList();
        this.checkedOn = topic.getCheckedOn();
        //이미지가 바뀌면 v가 바뀌어 브라우저 캐시를 새로 받는다
        this.imageUrl = topic.hasImage()
                ? imageBasePath + topic.getId() + "/image?v=" + topic.getImageUpdatedAt().toEpochSecond(ZoneOffset.UTC)
                : null;
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
