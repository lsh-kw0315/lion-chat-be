package com.lion.be.notification.domain.dto;

import com.lion.be.notification.domain.NotificationType;
import lombok.*;


public record NotificationEvent(Long fromUserId, Long toUserId, NotificationType type, Long targetId) {
}
