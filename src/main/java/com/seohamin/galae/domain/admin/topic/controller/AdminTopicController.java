package com.seohamin.galae.domain.admin.topic.controller;

import com.seohamin.galae.domain.admin.topic.dto.TopicCheckRequestDto;
import com.seohamin.galae.domain.admin.topic.service.AdminTopicService;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import lombok.RequiredArgsConstructor;
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

    //검증일 지정 API
    @PatchMapping("/topics/{topicId}/checked")
    public ResponseEntity<TopicResponseDto> updateChecked(
            @PathVariable("topicId") final String topicId,
            @RequestBody final TopicCheckRequestDto requestDto
    ) {
        return ResponseEntity.ok(adminTopicService.updateChecked(topicId, requestDto));
    }
}
