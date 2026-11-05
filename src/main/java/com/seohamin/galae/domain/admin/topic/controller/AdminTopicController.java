package com.seohamin.galae.domain.admin.topic.controller;

import com.seohamin.galae.domain.admin.topic.dto.AdminTopicRequestDto;
import com.seohamin.galae.domain.admin.topic.dto.TopicCheckRequestDto;
import com.seohamin.galae.domain.admin.topic.service.AdminTopicService;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import com.seohamin.galae.domain.feed.entity.image.TopicImage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminTopicController {

    private final AdminTopicService adminTopicService;

    //전체 주제 조회 API (보류 포함)
    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponseDto>> getTopics() {
        return ResponseEntity.ok(adminTopicService.getTopics());
    }

    //주제 수정 API (이미지)
    @PatchMapping("/topics/{topicId}")
    public ResponseEntity<TopicResponseDto> updateTopic(
            @PathVariable("topicId") final String topicId,
            @RequestBody final AdminTopicRequestDto requestDto
    ) {
        return ResponseEntity.ok(adminTopicService.updateTopic(topicId, requestDto));
    }

    //검증일 지정 API
    @PatchMapping("/topics/{topicId}/checked")
    public ResponseEntity<TopicResponseDto> updateChecked(
            @PathVariable("topicId") final String topicId,
            @RequestBody final TopicCheckRequestDto requestDto
    ) {
        return ResponseEntity.ok(adminTopicService.updateChecked(topicId, requestDto));
    }

    //주제 이미지 미리보기 API (보류 중인 주제 포함)
    @GetMapping("/topics/{topicId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("topicId") final String topicId) {
        final TopicImage image = adminTopicService.getImage(topicId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.getData());
    }
}
