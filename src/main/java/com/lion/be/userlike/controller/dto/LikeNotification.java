package com.lion.be.userlike.controller.dto;

import java.time.LocalDateTime;

public record LikeNotification(
	Long fromUserId,
	String fromUserName,
	String message,
	LocalDateTime timestamp
) {
}
