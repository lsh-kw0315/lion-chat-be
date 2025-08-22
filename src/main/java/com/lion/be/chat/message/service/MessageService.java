package com.lion.be.chat.message.service;

import com.lion.be.chat.message.domain.entity.ChatMessage;
import com.lion.be.chat.room.domain.MessageStatus;
import com.lion.be.chat.room.domain.entity.ChatRoom;
import com.lion.be.chat.room.domain.entity.ChatRoomUser;
import com.lion.be.chat.room.repository.ChatRoomUserRepository;
import com.lion.be.chat.room.service.ChatRoomPersistence;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.global.interceptor.StompInterceptor;
import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.service.NotificationWriteService;
import com.lion.be.user.domain.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatRoomUserRepository chatRoomUserRepository;
    private final MessagePublisher messagePublisher;
    private final MessageWriteService messageWriteService;
    private final ChatRoomPersistence chatRoomPersistence;
    private final NotificationWriteService notificationWriteService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    @ElapsedTime
    public void publishMessage(ChatMessage message, ChatRoom chatRoom, ChatRoomUser chatRoomSender) {
        messagePublisher.publishMessage(message);

        ChatRoomUser receiverChatRoomUser = findOpponentChatRoomUser(message.getChatRoomId(), chatRoomSender.getUser());
        Long receiverId =receiverChatRoomUser.getUser().getId();

        if(!redisTemplate.opsForSet().members(StompInterceptor.USER_ENDPOINT_KEY_PREFIX+receiverId.toString()).contains("/topic/chatroom/"+chatRoom.getId())){
            Notification notification = notificationWriteService.save(
                    chatRoomSender.getUser().getId(),
                    receiverId,
                    NotificationType.CHATTING,
                    chatRoom.getId()
            );
            applicationEventPublisher.publishEvent(
                    new NotificationEvent(
                            notification.getId(),
                            notification.getFromUserId(),
                            notification.getToUserId(),
                            notification.getType(),
                            notification.getTargetId()
                    )
            );
        }
        messageWriteService.updateMessageStatus(message, MessageStatus.PUBLISHED);
        chatRoomPersistence.updateChatRoomRecentMessage(chatRoom, message);
        chatRoomPersistence.updateChatRoomUserReadStatus(chatRoomSender, true);
        chatRoomPersistence.updateChatRoomUserReadStatus(receiverChatRoomUser, false);
    }

    public ChatRoomUser findOpponentChatRoomUser(Long chatRoomId, User sender) {
        return chatRoomUserRepository.findById_ChatRoomId(chatRoomId)
                .stream()
                .filter(cru -> !cru.getUser().getId().equals(sender.getId()))
                .findFirst().orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }
}
