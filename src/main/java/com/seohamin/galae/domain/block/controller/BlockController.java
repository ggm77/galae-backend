package com.seohamin.galae.domain.block.controller;

import com.seohamin.galae.domain.block.dto.BlockListRequestDto;
import com.seohamin.galae.domain.block.dto.BlockResponseDto;
import com.seohamin.galae.domain.block.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    //내 차단 목록 API
    @GetMapping("/users/me/blocks")
    public ResponseEntity<List<BlockResponseDto>> getBlocks(
            @AuthenticationPrincipal final Long userId
    ) {
        return ResponseEntity.ok(blockService.getBlocks(userId));
    }

    //차단 목록 전체 교체 API
    @PutMapping("/users/me/blocks")
    public ResponseEntity<List<BlockResponseDto>> replaceBlocks(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final BlockListRequestDto blockListRequestDto
    ) {
        return ResponseEntity.ok(blockService.replaceBlocks(blockListRequestDto.getUserIds(), userId));
    }

    //한 명 차단 API
    @PutMapping("/users/me/blocks/{targetId}")
    public ResponseEntity<Void> block(
            @PathVariable("targetId") final Long targetId,
            @AuthenticationPrincipal final Long userId
    ) {
        blockService.block(targetId, userId);
        return ResponseEntity.noContent().build();
    }

    //차단 해제 API
    @DeleteMapping("/users/me/blocks/{targetId}")
    public ResponseEntity<Void> unblock(
            @PathVariable("targetId") final Long targetId,
            @AuthenticationPrincipal final Long userId
    ) {
        blockService.unblock(targetId, userId);
        return ResponseEntity.noContent().build();
    }
}
