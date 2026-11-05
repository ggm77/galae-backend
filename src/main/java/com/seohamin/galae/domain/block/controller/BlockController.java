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

/**
 * 사용자 차단. 차단할 사람은 아이디(handle)로 지정한다
 */
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

    //차단 목록 전체 교체 API (아이디 목록)
    @PutMapping("/users/me/blocks")
    public ResponseEntity<List<BlockResponseDto>> replaceBlocks(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final BlockListRequestDto blockListRequestDto
    ) {
        return ResponseEntity.ok(blockService.replaceBlocks(blockListRequestDto.getHandles(), userId));
    }

    //한 명 차단 API
    @PutMapping("/users/me/blocks/{handle}")
    public ResponseEntity<Void> block(
            @PathVariable("handle") final String handle,
            @AuthenticationPrincipal final Long userId
    ) {
        blockService.block(handle, userId);
        return ResponseEntity.noContent().build();
    }

    //차단 해제 API
    @DeleteMapping("/users/me/blocks/{handle}")
    public ResponseEntity<Void> unblock(
            @PathVariable("handle") final String handle,
            @AuthenticationPrincipal final Long userId
    ) {
        blockService.unblock(handle, userId);
        return ResponseEntity.noContent().build();
    }
}
