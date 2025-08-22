package com.lion.be.usercard.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lion.be.global.aop.ElapsedTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.user.domain.Position;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.domain.entity.UserPhoto;
import com.lion.be.user.repository.UserRepository;
import com.lion.be.usercard.controller.dto.UserCardResponse;
import com.lion.be.usercard.util.UserCardFilterUtil;
import com.lion.be.userlike.service.UserLikesReadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCardReadService {

	private final UserViewHistoryService userViewHistoryService;
	private final UserCardFilterUtil userCardFilterUtil;
	private final UserRepository userRepository;
	private final UserLikesReadService userLikesReadService;
	@ElapsedTime
	public List<UserCardResponse> getCards(Long userId, int size, List<Long> excludeUserIds) {
		List<Long> allExcludeUserIds = userViewHistoryService.getExcludeUserIds(userId, excludeUserIds);
		List<User> recommendedUsers = userCardFilterUtil.getRecommendedUsers(userId, size, allExcludeUserIds);

		return convertToUserCardResponses(userId, recommendedUsers);
	}

	public UserCardResponse getUserCard(Long targetUserId, Long currentUserId) {
		User user = userRepository.fetchByIdWithPhotos(targetUserId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		boolean isLiked = false;
		if (currentUserId != null) {
			Set<Long> likedUserIds = userLikesReadService.getLikedUserIds(currentUserId, List.of(targetUserId));
			isLiked = likedUserIds.contains(targetUserId);
		}

		return UserCardResponse.from(user, isLiked);
	}

	public List<UserCardResponse> getCardsByPosition(Long userId, int size, List<Long> excludeUserIds, Position position) {
		List<Long> allExcludeUserIds = userViewHistoryService.getExcludeUserIds(userId, excludeUserIds);
		List<User> users = userCardFilterUtil.getRecommendedUsersByPosition(userId, size, allExcludeUserIds, position);

		return convertToUserCardResponses(userId, users);
	}

	private List<UserCardResponse> convertToUserCardResponses(Long currentUserId, List<User> users) {
		if (users.isEmpty()) {
			return List.of();
		}

		List<Long> userIds = users.stream()
			.map(User::getId)
			.toList();

		userViewHistoryService.recordViewedUsers(currentUserId, userIds);
		Set<Long> likedUserIds = userLikesReadService.getLikedUserIds(currentUserId, userIds);

		Map<Long, List<UserPhoto>> photosMap = userRepository.findPhotosMapByUserIds(userIds);

		return users.stream()
			.map(user -> {
				List<UserPhoto> photos = photosMap.getOrDefault(user.getId(), List.of());
				return UserCardResponse.from(user, photos, likedUserIds.contains(user.getId()));
			})
			.toList();
	}
}
