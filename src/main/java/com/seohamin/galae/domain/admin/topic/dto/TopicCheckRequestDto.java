package com.seohamin.galae.domain.admin.topic.dto;

import lombok.Getter;

import java.time.LocalDate;

/**
 * 생각 피드 주제의 검증일 (docs/VERIFICATION.md)
 * 날짜를 넣으면 노출, null이면 보류
 */
@Getter
public class TopicCheckRequestDto {

    private LocalDate checkedOn;
}
