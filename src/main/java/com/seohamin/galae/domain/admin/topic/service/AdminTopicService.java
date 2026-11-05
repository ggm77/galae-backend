package com.seohamin.galae.domain.admin.topic.service;

import com.seohamin.galae.domain.admin.topic.dto.AdminTopicRequestDto;
import com.seohamin.galae.domain.admin.topic.dto.TopicCheckRequestDto;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import com.seohamin.galae.domain.feed.entity.Topic;
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

    @Transactional
    public TopicResponseDto updateChecked(final String topicId, final TopicCheckRequestDto requestDto) {
        final Topic topic = findTopic(topicId);
        topic.updateCheckedOn(requestDto.getCheckedOn());
        return new TopicResponseDto(topic, 0, ADMIN_IMAGE_PATH);
    }

    /**
     * 주제 수정. 지금은 이미지만 (null이면 그대로, 빈 문자열이면 삭제, data URL이면 넣거나 바꿈)
     */
    @Transactional
    public TopicResponseDto updateTopic(final String topicId, final AdminTopicRequestDto requestDto) {
        final Topic topic = findTopic(topicId);
        final String image = requestDto.getImage();

        if (image != null) {
            if (image.isEmpty()) {
                topicImageRepository.findById(topicId).ifPresent(topicImageRepository::delete);
                topic.markImageUpdated(false);
            } else {
                final ImageUtil.DecodedImage decoded = ImageUtil.decode(image, MAX_IMAGE_BYTES);
                topicImageRepository.findById(topicId).ifPresentOrElse(
                        existing -> existing.update(decoded.bytes(), decoded.contentType()),
                        () -> topicImageRepository.save(new TopicImage(topic, decoded.bytes(), decoded.contentType()))
                );
                topic.markImageUpdated(true);
            }
        }

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

    private Topic findTopic(final String topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException(ExceptionCode.TOPIC_NOT_EXIST));
    }
}
