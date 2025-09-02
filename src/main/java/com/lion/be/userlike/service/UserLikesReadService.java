package com.lion.be.userlike.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.lion.be.notification.repository.NotificationRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.user.domain.entity.User;
import com.lion.be.usercard.controller.dto.UserCardResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLikesReadService {

	private final NotificationRepository notificationRepository;
	/**
	 * 카드 에서 현재 사용자가 좋아요한 사용자들 조회
	 */
	public Set<Long> getLikedUserIds(Long currentUserId, List<Long> targetUserIds) {
		if (currentUserId == null || targetUserIds.isEmpty()) {
			return Set.of();
		}

		return new HashSet<>(notificationRepository.findLikedUserIdsAmon(currentUserId, targetUserIds));
	}

	/**
	 * 로그인한 사용자가 좋아요 누른 사용자들 목록 조회
	 * @param userPrincipal 로그인한 사용자
	 * @return 좋아요 누른 사용자들의 카드 정보 목록
	 */
	public Page<UserCardResponse> getUsersWhoLiked(UserPrincipal userPrincipal, Pageable pageable) {
		Long userId = userPrincipal.getId();

		// userLikes 테이블에서 fromUserId로 조회
		Page<User> likedUsers = notificationRepository.fetchAllLikeUser(userId, pageable);

		return likedUsers
			.map(user -> UserCardResponse.from(user, true));
	}
}
