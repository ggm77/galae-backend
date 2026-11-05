package com.seohamin.galae.domain.admin.topic.service;

import com.seohamin.galae.domain.admin.topic.dto.TopicCheckRequestDto;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import com.seohamin.galae.domain.feed.entity.Topic;
import com.seohamin.galae.domain.feed.repository.TopicRepository;
import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTopicService {

    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public List<TopicResponseDto> getTopics() {
        return topicRepository.findAllByOrderByIdAsc().stream()
                .map(t -> new TopicResponseDto(t, 0))
                .toList();
    }

    @Transactional
    public TopicResponseDto updateChecked(final String topicId, final TopicCheckRequestDto requestDto) {
        final Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new CustomException(ExceptionCode.TOPIC_NOT_EXIST));
        topic.updateCheckedOn(requestDto.getCheckedOn());
        return new TopicResponseDto(topic, 0);
    }
}
