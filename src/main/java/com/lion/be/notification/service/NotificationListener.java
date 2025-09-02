package com.lion.be.notification.service;

import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.image.domain.entity.Image;
import com.lion.be.image.repository.ImageRepository;
import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.dto.NotificationResponse;
import com.lion.be.notification.domain.dto.PostLikeEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.UserRepository;
import com.lion.be.user.service.UserReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {
    private final NotificationRepository notificationRepository;
    private final ImageRepository imageRepository;
    private final UserReadService userReadService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMessage(NotificationEvent event){
        Notification notification = notificationRepository.save(
                new Notification(
                        event.fromUserId(),
                        event.toUserId(),
                        event.targetId(),
                        event.type()
                )
        );

        sendWebsocketAlarm(event, notification);

    }

    private void sendWebsocketAlarm(NotificationEvent event, Notification notification) {
        User receiver = userReadService.fetchById(event.toUserId());
        User sender = userRepository.fetchByIdWithPhotos(event.fromUserId()).orElseThrow(
                ()->new CustomException(ErrorCode.USER_NOT_FOUND)
        );
        String destination = "/topic/alarm/"+receiver.getId();
        Optional<Image> senderImage = imageRepository.fetchByUserId(sender.getId());
        String imageUrl = senderImage.isPresent() ? senderImage.get().getImageUrl() : "https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png";

        NotificationResponse message = NotificationResponse.toResponse(
                notification.getId(),
                sender.getId(),
                sender.getNickname(),
                receiver.getId(),
                receiver.getNickname(),
                event.type(),
                notification.getCreatedAt(),
                imageUrl,
                event.targetId()
        );

        messagingTemplate.convertAndSend(destination, message);
    }

    @EventListener
    @Async
    public void processPostLikeEvent(PostLikeEvent postLikeEvent){
        Notification notification = notificationRepository.save(
                new Notification(
                        postLikeEvent.fromUserId(),
                        postLikeEvent.toUserId(),
                        postLikeEvent.targetId(),
                        postLikeEvent.type()
                )
        );

        sendWebsocketAlarm(
                new NotificationEvent(
                        postLikeEvent.fromUserId(),
                        postLikeEvent.toUserId(),
                        postLikeEvent.type(),
                        postLikeEvent.targetId()
                ), notification);
    }
}
