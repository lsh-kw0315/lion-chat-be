package com.lion.be.userlike.service;

import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLikesWriteService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 좋아요 토글 (생성/삭제)
	 */
	@Transactional
	public boolean toggleLike(Long currentUserId, Long targetUserId) {
		if (currentUserId.equals(targetUserId)) {
			throw new CustomException(ErrorCode.USER_CAN_NOT_LIKE_HIMSELF);
		}

		if (notificationRepository.extractProfileLike(currentUserId, targetUserId).isPresent()) {
			// 좋아요 취소
			notificationRepository.deleteNotification(currentUserId, targetUserId);
			return false;
		} else {
			eventPublisher.publishEvent(
					new NotificationEvent(currentUserId, targetUserId, NotificationType.PROFILE_LIKE, currentUserId)
			);

			return true;
		}
	}
}
