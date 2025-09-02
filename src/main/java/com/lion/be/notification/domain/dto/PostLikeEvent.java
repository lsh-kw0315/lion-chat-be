package com.lion.be.notification.domain.dto;

import com.lion.be.notification.domain.NotificationType;

public record PostLikeEvent(Long fromUserId, Long toUserId, NotificationType type, Long targetId) {
}
