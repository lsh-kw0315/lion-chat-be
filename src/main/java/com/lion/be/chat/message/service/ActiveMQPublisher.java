package com.lion.be.chat.message.service;

import com.lion.be.chat.message.domain.dto.ChatMessageResponse;
import com.lion.be.chat.message.domain.entity.ChatMessage;
import com.lion.be.chat.room.repository.ChatRoomUserRepository;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.image.domain.entity.Image;
import com.lion.be.image.repository.ImageRepository;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveMQPublisher implements MessagePublisher {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ImageRepository imageRepository;

    private static final String DESTINATION = "/topic/chatroom/";

    @Override
    public void publishMessage(ChatMessage message) {
        String destination = DESTINATION + message.getChatRoomId();
        User sender = userRepository.findById(message.getSenderId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Optional<Image> senderImage = imageRepository.fetchByUserId(sender.getId());
        String imageUrl = senderImage.isPresent() ? senderImage.get().getImageUrl() : "https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png";
        ChatMessageResponse response = ChatMessageResponse.toResponse(message, sender, imageUrl, false);
        messagingTemplate.convertAndSend(destination, response);
    }
}
