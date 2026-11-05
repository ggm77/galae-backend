package com.seohamin.galae.domain.admin.topic.dto;

import lombok.Getter;

/**
 * 생각 탭 주제 수정 (관리자)
 * null인 필드는 바꾸지 않는다
 */
@Getter
public class AdminTopicRequestDto {

    //주제 이미지 data URL (data:image/jpeg|png|webp;base64,...), 1MB 이하. 빈 문자열이면 삭제
    private String image;
}
