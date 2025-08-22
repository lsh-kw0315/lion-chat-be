package com.lion.be.usercard.controller;

import java.util.List;

import com.lion.be.global.aop.ElapsedTime;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.user.domain.Position;
import com.lion.be.usercard.controller.dto.UserCardResponse;
import com.lion.be.usercard.service.UserCardReadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCardController {

	private final UserCardReadService userCardReadService;

	@GetMapping("/profile")
	public ResponseEntity<UserCardResponse> getMyCard(
		@AuthenticationPrincipal UserPrincipal userPrincipal
		){
		UserCardResponse cards = userCardReadService.getUserCard(
			userPrincipal.getId(), null
		);
		return ResponseEntity.ok(cards);
	}

	@GetMapping("/profile/{userId}")
	public ResponseEntity<UserCardResponse> getUserCard(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@PathVariable Long userId
	){
		UserCardResponse cards = userCardReadService.getUserCard(
			userId, userPrincipal.getId()
		);
		return ResponseEntity.ok(cards);
	}

	@GetMapping("/cards/list")
	@ElapsedTime
	public ResponseEntity<List<UserCardResponse>> getCards(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) List<Long> excludeUserIds
	) {
		log.info("사용자 {}가 카드 조회 요청 - size: {}, 제외할 사용자 수: {}",
			userPrincipal.getId(), size,
			excludeUserIds != null ? excludeUserIds.size() : 0);

		List<UserCardResponse> cards = userCardReadService.getCards(
			userPrincipal.getId(),
			size,
			excludeUserIds
		);
		return ResponseEntity.ok(cards);
	}

	@GetMapping("/cards/category")
	public ResponseEntity<List<UserCardResponse>> getCardsByPosition(
		@AuthenticationPrincipal UserPrincipal userPrincipal,
		@RequestParam(defaultValue = "10") int size,
		@RequestParam(required = false) List<Long> excludeUserIds,
		@RequestParam(required = false) Position position
	){
		List<UserCardResponse> cards = userCardReadService.getCardsByPosition(
			userPrincipal.getId(), size, excludeUserIds, position
		);
		return ResponseEntity.ok(cards);
	}
}
