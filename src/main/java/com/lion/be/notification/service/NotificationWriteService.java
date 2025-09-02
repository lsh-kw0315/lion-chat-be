package com.lion.be.notification.service;

import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.lion.be.userlike.controller.dto.LikeNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWriteService {

	private final NotificationRepository notificationRepository;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional
	public Notification save(Long fromUserId, Long toUserId, NotificationType type, Long targetId){
		return notificationRepository.save(
				new Notification(fromUserId, toUserId, targetId, type)
		);
	}

	@Transactional
	public void sendToUser(Long userId, LikeNotification notification) {
		try {
			messagingTemplate.convertAndSendToUser(
				userId.toString(),
				"/topic/",
				notification
			);
		} catch (Exception e) {
			log.error("좋아요 알림 전송 실패: userId={}", userId, e);
		}
	}
}
