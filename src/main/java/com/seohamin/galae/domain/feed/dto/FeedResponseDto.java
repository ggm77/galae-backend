package com.seohamin.galae.domain.feed.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 오늘의 생각 피드. 하루 다섯 주제에서 멈춘다 (docs/PURPOSE.md)
 */
@Getter
public class FeedResponseDto {

    private final LocalDate date;
    private final List<TopicResponseDto> topics;

    public FeedResponseDto(final LocalDate date, final List<TopicResponseDto> topics) {
        this.date = date;
        this.topics = topics;
    }
}
