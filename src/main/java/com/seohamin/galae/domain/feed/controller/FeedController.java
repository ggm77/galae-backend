package com.seohamin.galae.domain.feed.controller;

import com.seohamin.galae.domain.feed.dto.FeedResponseDto;
import com.seohamin.galae.domain.feed.dto.TopicResponseDto;
import com.seohamin.galae.domain.feed.entity.image.TopicImage;
import com.seohamin.galae.domain.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    //오늘의 생각 피드 API
    @GetMapping("/feed")
    public ResponseEntity<FeedResponseDto> getTodayFeed() {
        return ResponseEntity.ok(feedService.getTodayFeed());
    }

    //주제 조회 API
    @GetMapping("/feed/topics/{topicId}")
    public ResponseEntity<TopicResponseDto> getTopic(@PathVariable("topicId") final String topicId) {
        return ResponseEntity.ok(feedService.getTopic(topicId));
    }

    //주제 이미지 API (보이는 주제만, 인증 없이 - img 태그용)
    @GetMapping("/feed/topics/{topicId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("topicId") final String topicId) {
        final TopicImage image = feedService.getImage(topicId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(image.getData());
    }
}
