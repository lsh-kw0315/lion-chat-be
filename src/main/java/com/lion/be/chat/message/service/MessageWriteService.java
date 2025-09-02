package com.lion.be.chat.message.service;

import com.lion.be.chat.message.domain.entity.ChatMessage;
import com.lion.be.chat.message.repository.ChatMessageRepository;
import com.lion.be.chat.room.domain.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageWriteService {

    private final ChatMessageRepository chatMessageRepository;

    public void updateMessageStatus(ChatMessage message, MessageStatus status) {
        message.updateMessageStatus(status);
        chatMessageRepository.save(message);
        log.debug("메시지 상태를 {}로 변경합니다.", status);
    }

    public void updateMessagesAsRead(Slice<ChatMessage> messages, Long userId) {
        List<ObjectId> unreadMessageIds = messages.getContent().stream()
                .filter(message -> !message.getSenderId().equals(userId))
                .map(ChatMessage::getId)
                .collect(Collectors.toList());

        if (!unreadMessageIds.isEmpty()) {
            chatMessageRepository.markMessagesAsRead(unreadMessageIds);
        }
    }
}
