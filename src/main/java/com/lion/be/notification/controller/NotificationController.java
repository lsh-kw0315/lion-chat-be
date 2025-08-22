package com.lion.be.notification.controller;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.notification.domain.dto.NotificationResponse;
import com.lion.be.notification.service.NotificationReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationReadService notificationReadService;

    @ElapsedTime
    @GetMapping("/api/notifications")
    public ResponseEntity<Slice<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value="lastId", required = false) Long lastId,
            @RequestParam(value="size", defaultValue = "20") int size
            ){
        return ResponseEntity.ok(
                notificationReadService.fetchAllAlarm(userPrincipal.getId(), lastId, size)
        );
    }
}
