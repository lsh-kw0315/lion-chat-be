package com.lion.be.userlike.controller;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.usercard.controller.dto.UserCardResponse;
import com.lion.be.userlike.controller.dto.LikeResponse;
import com.lion.be.userlike.service.UserLikesReadService;
import com.lion.be.userlike.service.UserLikesWriteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/likes")
public class UserLikesController {

	private final UserLikesWriteService userLikesWriteService;
	private final UserLikesReadService userLikesReadService;

	/**
	 * 좋아요 토글 (생성/삭제)
	 * @param toUserId 좋아요 받는 유저
	 * @param userPrincipal 좋아요 보내는 유저
	 * @return 좋아요 상태
	 */
	@PostMapping("/{toUserId}")
	public ResponseEntity<LikeResponse> toggleLike(
		@PathVariable Long toUserId,
		@AuthenticationPrincipal UserPrincipal userPrincipal) {

		boolean isLiked = userLikesWriteService.toggleLike(userPrincipal.getId(), toUserId);

		return ResponseEntity.ok(new LikeResponse(isLiked));
	}

	/**
	 * 로그인한 사용자가 좋아요 누른 사용자들 목록 조회
	 * @param userPrincipal 로그인한 사용자
	 * @return 좋아요 누른 사용자들 목록
	 */
	@GetMapping("/lists")
	public ResponseEntity<Page<UserCardResponse>> getMyLikedUsers(
		@PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
		@AuthenticationPrincipal UserPrincipal userPrincipal) {

		Page<UserCardResponse> likedUsers = userLikesReadService.getUsersWhoLiked(userPrincipal, pageable);
		return ResponseEntity.ok(likedUsers);
	}
}
