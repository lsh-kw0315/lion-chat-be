package com.lion.be.chat.message.service;

import com.lion.be.chat.message.domain.dto.ChatMessageRequest;
import com.lion.be.chat.message.domain.dto.ChatMessageResponse;
import com.lion.be.chat.message.domain.entity.ChatMessage;
import com.lion.be.chat.message.repository.ChatMessageRepository;
import com.lion.be.chat.room.domain.MessageStatus;
import com.lion.be.chat.room.domain.entity.ChatRoom;
import com.lion.be.chat.room.domain.entity.ChatRoomUser;
import com.lion.be.chat.room.repository.ChatRoomRepository;
import com.lion.be.chat.room.repository.ChatRoomUserRepository;
import com.lion.be.chat.room.service.ChatRoomPersistence;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.image.domain.entity.Image;
import com.lion.be.image.repository.ImageRepository;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.persistence.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageUseCase {

    private static final String DEFAULT_IMAGE_URL = "https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png";

    private final UserJpaRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final MessageService messageService;
    private final MessageWriteService messageWriteService;
    private final ChatRoomPersistence chatRoomPersistence;
    private final ImageRepository imageRepository;
    private final MessageReadService messageReadService;

    @ElapsedTime
    public void sendMessage(ChatMessageRequest request, Long senderId) {
        ChatRoom chatRoom = chatRoomRepository.findById(request.chatRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatRoomUser chatRoomSender = chatRoomUserRepository.findById_ChatRoomIdAndId_UserId(request.chatRoomId(), senderId);
        ChatMessage message = ChatMessageRequest.fromRequest(request, chatRoomSender.getUser());

        messageWriteService.updateMessageStatus(message, MessageStatus.PENDING);
        try {
            messageService.publishMessage(message, chatRoom, chatRoomSender);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.MESSAGE_PUBLISH_FAILED);
        }
    }

    @Transactional
    @ElapsedTime
    public void processReadAck(String messageId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ChatMessage message = chatMessageRepository.findById(new ObjectId(messageId))
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));
        messageWriteService.updateMessageStatus(message, MessageStatus.DELIVERED);
        ChatRoomUser receiverChatRoomUser = messageService.findOpponentChatRoomUser(message.getChatRoomId(), user);
        chatRoomPersistence.updateChatRoomUserReadStatus(receiverChatRoomUser, true);
    }

    @ElapsedTime
    public List<ChatMessageResponse> findMessagesByIdAndLastId(Long roomId, String lastId, Long userId) {
        Slice<ChatMessage> messages = messageReadService.getMessages(roomId, lastId);
        boolean isEnd = !messages.hasNext();

        messageWriteService.updateMessagesAsRead(messages, userId);

        Set<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .collect(Collectors.toSet());

        Map<Long, User> users = userRepository.findByIdIn(senderIds).stream().collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, String> userPhotoMap = new HashMap<>();

        List<Image> images = imageRepository.fetchAllByUserId(senderIds.stream().toList());
        for (Image image : images) {
            userPhotoMap.put(image.getUploaderId(), image.getImageUrl());
        }

        ChatRoomUser chatRoomUser = chatRoomUserRepository.findById_ChatRoomIdAndId_UserId(roomId, userId);
        chatRoomPersistence.updateChatRoomUserReadStatus(chatRoomUser, true);

        List<ChatMessage> messageList = messages.getContent();
        int lastIndex = messageList.size() - 1;
        return IntStream.range(0, messageList.size())
                .mapToObj(i -> {
                    ChatMessage message = messageList.get(i);
                    String mappedImage = userPhotoMap.get(message.getSenderId());
                    String nickname = users.get(message.getSenderId()).getNickname();
                    String imageUrl = mappedImage != null ? mappedImage : DEFAULT_IMAGE_URL;

                    boolean isLast = (i == lastIndex) && isEnd;

                    return new ChatMessageResponse(
                            message.getId().toString(),
                            message.getChatRoomId(),
                            message.getSenderId(),
                            nickname,
                            imageUrl,
                            message.getCreatedAt(),
                            message.getContent(),
                            isLast
                    );
                })
                .collect(Collectors.toList());
    }
}
