package com.lion.be.feed.controller;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.feed.service.FeedLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedLikeController {

    private final FeedLikeService feedLikeService;

    @PostMapping("/api/feeds/{feedId}/like")
    public ResponseEntity<Void> likeFeed(@PathVariable Long feedId,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        feedLikeService.likeFeed(feedId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/feeds/{feedId}/like")
    public ResponseEntity<Void> unlikeFeed(@PathVariable Long feedId,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        feedLikeService.unlikeFeed(feedId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/feeds/{feedId}/like/rdb")
    public ResponseEntity<Void> likeFeedRDB(@PathVariable Long feedId,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        feedLikeService.likeFeedRDB(feedId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/feeds/{feedId}/like/rdb")
    public ResponseEntity<Void> unlikeFeedRDB(@PathVariable Long feedId,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        feedLikeService.unlikeFeedRDB(feedId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

}