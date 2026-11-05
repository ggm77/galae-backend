package com.seohamin.galae.domain.feed.service;

import com.seohamin.galae.domain.comment.repository.CommentRepository;
import com.seohamin.galae.domain.feed.dto.FeedResponseDto;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import com.seohamin.galae.domain.feed.entity.Topic;
import com.seohamin.galae.domain.feed.entity.image.TopicImage;
import com.seohamin.galae.domain.feed.repository.image.TopicImageRepository;
import com.seohamin.galae.domain.feed.repository.TopicRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.time.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 생각 탭
 * 검증을 마친 주제만 보이고, galae.feed.include-unchecked=true면 검증 전 주제도 보인다 (개발용)
 * 하루 5개를 날짜에 따라 돌아가며 보여 준다 (프론트 src/topics.js와 같은 규칙)
 */
@Service
@Transactional(readOnly = true)
public class FeedService {

    public static final int TOPICS_PER_DAY = 5;

    private final TopicRepository topicRepository;
    private final TopicImageRepository topicImageRepository;
    private final CommentRepository commentRepository;
    private final TimeProvider timeProvider;
    private final boolean includeUnchecked;

    public FeedService(
            final TopicRepository topicRepository,
            final TopicImageRepository topicImageRepository,
            final CommentRepository commentRepository,
            final TimeProvider timeProvider,
            @Value("${galae.feed.include-unchecked:false}") final boolean includeUnchecked
    ) {
        this.topicRepository = topicRepository;
        this.topicImageRepository = topicImageRepository;
        this.commentRepository = commentRepository;
        this.timeProvider = timeProvider;
        this.includeUnchecked = includeUnchecked;
    }

    /**
     * 오늘의 피드 (최대 5개)
     */
    public FeedResponseDto getTodayFeed() {
        final LocalDate today = timeProvider.today();
        final List<Topic> visible = visibleTopics();

        final List<Topic> topics;
        if (visible.size() <= TOPICS_PER_DAY) {
            topics = visible;
        } else {
            final long dayNo = today.toEpochDay();
            topics = IntStream.range(0, TOPICS_PER_DAY)
                    .mapToObj(i -> visible.get((int) ((dayNo * TOPICS_PER_DAY + i) % visible.size())))
                    .toList();
        }

        final Map<String, Long> counts = commentCounts(topics.stream().map(Topic::getId).toList());
        return new FeedResponseDto(
                today,
                topics.stream().map(t -> new TopicResponseDto(t, counts.getOrDefault(t.getId(), 0L))).toList()
        );
    }

    /**
     * 주제 하나 (공유 링크로 들어온 경우 등)
     */
    public TopicResponseDto getTopic(final String topicId) {
        final Topic topic = getVisibleTopic(topicId);
        return new TopicResponseDto(topic, commentCounts(List.of(topicId)).getOrDefault(topicId, 0L));
    }

    /**
     * 주제 이미지 (보이는 주제만. img 태그로 불러오므로 인증 없이)
     */
    public TopicImage getImage(final String topicId) {
        getVisibleTopic(topicId);
        return topicImageRepository.findById(topicId)
                .orElseThrow(() -> new CustomException(ExceptionCode.PHOTO_NOT_EXIST));
    }

    /**
     * 보이는 주제를 찾는 메서드. 댓글에서도 쓴다
     */
    public Topic getVisibleTopic(final String topicId) {
        return topicRepository.findById(topicId)
                .filter(t -> includeUnchecked || t.getCheckedOn() != null)
                .orElseThrow(() -> new CustomException(ExceptionCode.TOPIC_NOT_EXIST));
    }

    private List<Topic> visibleTopics() {
        return includeUnchecked
                ? topicRepository.findAllByOrderByIdAsc()
                : topicRepository.findAllByCheckedOnIsNotNullOrderByIdAsc();
    }

    private Map<String, Long> commentCounts(final List<String> topicIds) {
        final Map<String, Long> counts = new HashMap<>();
        if (topicIds.isEmpty()) {
            return counts;
        }
        for (final Object[] row : commentRepository.countByTopicIds(topicIds)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
}
