package com.lion.be.feed.controller;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.feed.controller.dto.FeedSaveResponse;
import com.lion.be.feed.domain.dto.FeedDto;
import com.lion.be.feed.domain.dto.FeedResponse;
import com.lion.be.feed.domain.dto.FeedUpdateRequest;
import com.lion.be.feed.domain.dto.FeedWriteRequest;
import com.lion.be.feed.service.FeedReadService;
import com.lion.be.feed.service.FeedWriteService;
import com.lion.be.global.aop.CheckRateLimitFeed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedReadService feedReadService;
    private final FeedWriteService feedWriteService;

    @GetMapping("/api/feeds")
    public ResponseEntity<Slice<FeedResponse>> showRecentFeeds(
            @RequestParam(value = "lastId", required = false) Long lastId,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // ✨ 유저 정보 추가
        Long currentUserId = (userPrincipal != null) ? userPrincipal.getId() : null;
        if (lastId != null && lastId > 0) {
            return ResponseEntity.ok(feedReadService.getRecentFeedsAfter(lastId, size, currentUserId));
        }
        return ResponseEntity.ok(feedReadService.getRecentFeedsFirst(size, currentUserId));
    }

    @GetMapping("/api/feeds/hot")
    public ResponseEntity<Slice<FeedResponse>> showHotFeeds(
            @RequestParam(value = "lastLikeCount", required = false) Long lastLikeCount,
            @RequestParam(value = "lastId", required = false) Long lastId,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // ✨ 유저 정보 추가
        Long currentUserId = (userPrincipal != null) ? userPrincipal.getId() : null;
        if (lastLikeCount != null && lastId != null && lastLikeCount >= 0 && lastId > 0) {
            return ResponseEntity.ok(feedReadService.getHotFeedsAfter(lastLikeCount, lastId, size, currentUserId));
        }
        return ResponseEntity.ok(feedReadService.getHotFeedsFirst(size, currentUserId));
    }

    @DeleteMapping("/api/feeds/{feedId}")
    public void deleteFeed(@PathVariable("feedId") Long feedId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getId();
        feedWriteService.deleteFeed(currentUserId, feedId);
    }

    @PostMapping("/api/feeds")
    @CheckRateLimitFeed
    public ResponseEntity<FeedSaveResponse> writeFeed(@RequestBody FeedWriteRequest feedWriteRequest,
                                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        FeedSaveResponse response = feedWriteService.writeFeed(feedWriteRequest.getTitle(),
                feedWriteRequest.getContent(), userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/feeds/{feedId}")
    public void modifyFeed(@PathVariable("feedId") Long feedId,
                           @RequestBody FeedUpdateRequest feedUpdateRequest,
                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        feedWriteService.updateFeed(feedId, feedUpdateRequest.getTitle(), feedUpdateRequest.getContent(), userId);
    }

    @GetMapping("/api/feeds/{feedId}")
    public ResponseEntity<FeedDto> getFeedById(
            @PathVariable("feedId") Long feedId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // ✨ 유저 정보 추가
        // ✨ 상세 조회 시에도 실시간 좋아요 정보를 반영하도록 수정 (ReadService에서 처리하는 것이 더 좋음)
        // 이 부분은 FeedReadService에 별도 메서드를 만들어 처리하는 것을 권장합니다.
        // 현재 구조를 유지하기 위해 Controller에서 간단히 구현합니다.
        Long currentUserId = (userPrincipal != null) ? userPrincipal.getId() : null;
        FeedResponse feedResponse = feedReadService.getFeedResponseById(feedId, currentUserId); // ✨ 새로운 서비스 메서드 호출
        return ResponseEntity.ok(feedResponse.getFeed());
    }

    @GetMapping("/api/feeds/me")
    public ResponseEntity<Slice<FeedResponse>> getMyFeeds(
            @RequestParam(value = "lastId", required = false) Long lastId,
            @RequestParam(value = "size", required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getId();
        if (lastId != null && lastId > 0) {
            return ResponseEntity.ok(feedReadService.getMyFeedsAfter(currentUserId, lastId, size));
        }
        return ResponseEntity.ok(feedReadService.getMyFeedsFirst(currentUserId, size));
    }

}
