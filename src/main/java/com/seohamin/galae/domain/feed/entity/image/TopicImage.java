package com.seohamin.galae.domain.feed.entity.image;

import com.seohamin.galae.domain.feed.entity.Topic;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 생각 탭 주제 이미지 (주제당 하나)
 * 피드를 불러올 때 이미지 바이트까지 읽지 않도록 topic 테이블과 나눠 둔다
 * PK가 주제 ID와 같다 (1:1)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "topic_image")
public class TopicImage {

    @Id
    @Column(name = "topic_id", length = 20)
    private String topicId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    //길이를 주지 않으면 MariaDB에서 tinyblob(255바이트)이 되므로 지정한다 → mediumblob
    @Lob
    @Column(nullable = false, length = 2_000_000)
    private byte[] data;

    //image/jpeg, image/png, image/webp
    @Column(length = 20, nullable = false)
    private String contentType;

    public TopicImage(final Topic topic, final byte[] data, final String contentType) {
        this.topic = topic;
        this.data = data;
        this.contentType = contentType;
    }

    public void update(final byte[] data, final String contentType) {
        this.data = data;
        this.contentType = contentType;
    }
}
