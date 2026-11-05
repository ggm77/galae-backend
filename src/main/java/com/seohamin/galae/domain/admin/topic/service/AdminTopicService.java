package com.seohamin.galae.domain.admin.topic.service;

import com.seohamin.galae.domain.admin.topic.dto.AdminTopicRequestDto;
import com.seohamin.galae.domain.admin.topic.dto.TopicCheckRequestDto;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import com.seohamin.galae.domain.feed.entity.Topic;
import com.seohamin.galae.domain.feed.entity.TopicSource;
import com.seohamin.galae.domain.feed.entity.image.TopicImage;
import com.seohamin.galae.domain.feed.repository.TopicRepository;
import com.seohamin.galae.domain.feed.repository.image.TopicImageRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import com.seohamin.galae.global.util.ImageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 관리자: 생각 탭 주제 추가·수정·검증일·이미지
 * 새 주제는 보류(검증 전)로 들어가고, 검증일을 지정해야 피드에 나온다 (docs/VERIFICATION.md)
 */
@Service
@RequiredArgsConstructor
public class AdminTopicService {

    //주제 이미지 최대 크기 (관리자 페이지에서 1080px로 줄여 올린다)
    public static final int MAX_IMAGE_BYTES = 1024 * 1024;

    //관리자 화면은 보류 중인 주제의 이미지도 보도록 관리자용 경로를 쓴다
    private static final String ADMIN_IMAGE_PATH = "/api/v1/admin/topics/";

    private final TopicRepository topicRepository;
    private final TopicImageRepository topicImageRepository;

    @Transactional(readOnly = true)
    public List<TopicResponseDto> getTopics() {
        return topicRepository.findAllByOrderByIdAsc().stream()
                .map(t -> new TopicResponseDto(t, 0, ADMIN_IMAGE_PATH))
                .toList();
    }

    /**
     * 주제 추가. 보류(검증 전)로 들어간다
     */
    @Transactional
    public TopicResponseDto createTopic(final AdminTopicRequestDto requestDto) {
        if (topicRepository.existsById(requestDto.getId())) {
            throw new CustomException(ExceptionCode.TOPIC_ALREADY_EXIST);
        }

        final Topic topic = topicRepository.save(Topic.builder()
                .id(requestDto.getId())
                .category(requestDto.getCategory().trim())
                .title(requestDto.getTitle().trim())
                .cards(cards(requestDto.getCards()))
                .sources(sources(requestDto.getSources()))
                .checkedOn(null)
                .build());

        if (requestDto.getImage() != null && !requestDto.getImage().isEmpty()) {
            applyImage(topic, requestDto.getImage());
        }

        return new TopicResponseDto(topic, 0, ADMIN_IMAGE_PATH);
    }

    /**
     * 주제 수정. null인 필드는 그대로
     * 검증을 마친 주제의 내용(분류·제목·카드·출처)을 고치면 다시 확인해야 하므로 보류로 돌아간다.
     * 이미지는 사실 주장이 아니라서 바꿔도 검증일을 유지한다
     */
    @Transactional
    public TopicResponseDto updateTopic(final String topicId, final AdminTopicRequestDto requestDto) {
        final Topic topic = findTopic(topicId);

        Optional.ofNullable(requestDto.getCategory()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(topic::updateCategory);
        Optional.ofNullable(requestDto.getTitle()).map(String::trim).filter(s -> !s.isEmpty()).ifPresent(topic::updateTitle);
        if (requestDto.getCards() != null) {
            topic.replaceCards(cards(requestDto.getCards()));
        }
        if (requestDto.getSources() != null) {
            topic.replaceSources(sources(requestDto.getSources()));
        }
        if (requestDto.changesContent()) {
            topic.updateCheckedOn(null);
        }

        if (requestDto.getImage() != null) {
            applyImage(topic, requestDto.getImage());
        }

        return new TopicResponseDto(topic, 0, ADMIN_IMAGE_PATH);
    }

    @Transactional
    public TopicResponseDto updateChecked(final String topicId, final TopicCheckRequestDto requestDto) {
        final Topic topic = findTopic(topicId);
        topic.updateCheckedOn(requestDto.getCheckedOn());
        return new TopicResponseDto(topic, 0, ADMIN_IMAGE_PATH);
    }

    /**
     * 주제 이미지 (보류 중인 주제 포함, 관리자 미리보기용)
     */
    @Transactional(readOnly = true)
    public TopicImage getImage(final String topicId) {
        findTopic(topicId);
        return topicImageRepository.findById(topicId)
                .orElseThrow(() -> new CustomException(ExceptionCode.PHOTO_NOT_EXIST));
    }

    /**
     * 빈 문자열이면 삭제, data URL이면 넣거나 바꾼다
     */
    private void applyImage(final Topic topic, final String image) {
        if (image.isEmpty()) {
            topicImageRepository.findById(topic.getId()).ifPresent(topicImageRepository::delete);
            topic.markImageUpdated(false);
            return;
        }

        final ImageUtil.DecodedImage decoded = ImageUtil.decode(image, MAX_IMAGE_BYTES);
        topicImageRepository.findById(topic.getId()).ifPresentOrElse(
                existing -> existing.update(decoded.bytes(), decoded.contentType()),
                () -> topicImageRepository.save(new TopicImage(topic, decoded.bytes(), decoded.contentType()))
        );
        topic.markImageUpdated(true);
    }

    private static List<String> cards(final List<String> cards) {
        return cards.stream().map(String::trim).toList();
    }

    private static List<TopicSource> sources(final List<AdminTopicRequestDto.SourceItem> sources) {
        return Optional.ofNullable(sources).orElse(List.of()).stream()
                .map(s -> new TopicSource(s.getLabel().trim(), s.getUrl() == null || s.getUrl().isBlank() ? null : s.getUrl().trim()))
                .toList();
    }

    private Topic findTopic(final String topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException(ExceptionCode.TOPIC_NOT_EXIST));
    }
}
