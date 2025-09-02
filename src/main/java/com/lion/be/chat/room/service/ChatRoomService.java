package com.lion.be.chat.room.service;

import com.lion.be.chat.message.service.MessageService;
import com.lion.be.chat.room.domain.dto.ChatRoomInitResponse;
import com.lion.be.chat.room.domain.dto.ChatRoomParticipantsInfoResponse;
import com.lion.be.chat.room.domain.dto.ChatRoomResponse;
import com.lion.be.chat.room.domain.entity.ChatRoom;
import com.lion.be.chat.room.domain.entity.ChatRoomUser;
import com.lion.be.chat.room.repository.ChatRoomRepository;
import com.lion.be.chat.room.repository.ChatRoomUserRepository;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationRepository notificationRepository;
    private final MessageService messageService;

    @Transactional
    public ChatRoomInitResponse findOrCreateChatRoom(Long senderId, Long receiverId) {

        Optional<Long> chatRoomId = chatRoomUserRepository.findChatRoomIdByTwoUserIds(senderId, receiverId);
        if (chatRoomId.isPresent()) {
            return ChatRoomInitResponse.toResponse(
                    chatRoomId.get()
            );
        } else {
            ChatRoom chatRoom = new ChatRoom();

            User user1 = userRepository.findById(senderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            User user2 = userRepository.findById(receiverId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            ChatRoomUser user1ChatRoomUser = ChatRoomUser.create(chatRoom, user1);
            ChatRoomUser user2ChatRoomUser = ChatRoomUser.create(chatRoom, user2);
            chatRoom.addUser(user1ChatRoomUser);
            chatRoom.addUser(user2ChatRoomUser);
            chatRoomRepository.save(chatRoom);

            applicationEventPublisher.publishEvent(
                    new NotificationEvent(senderId, receiverId, NotificationType.CHATROOM, chatRoom.getId())
            );

            return ChatRoomInitResponse.toResponse(
                    chatRoom.getId()
            );
        }
    }

    public ChatRoomParticipantsInfoResponse findChatRoomParticipants(Long userId, Long chatRoomId) {
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ChatRoomUser receiver = messageService.findOpponentChatRoomUser(chatRoomId, sender);
        return ChatRoomParticipantsInfoResponse.toResponse(
                userId,
                receiver.getUser().getId(),
                sender.getNickname(),
                receiver.getUser().getNickname()
        );
    }

    /**
     * 채팅방에 입장할 유저 2명의 정보를 저장합니다.
     *
     * @param user1
     * @param user2
     * @return 생성된 유저 리스트
     */
    public List<ChatRoomUser> createChatRoomUsers(ChatRoomUser user1, ChatRoomUser user2) {
        chatRoomUserRepository.save(user1);
        chatRoomUserRepository.save(user2);
        return List.of(user1, user2);
    }

    /**
     * 생성할 채팅방을 저장합니다.
     *
     * @param chatRoom
     * @return 생성된 채팅방 객체
     */
    public ChatRoom createChatRoom(ChatRoom chatRoom) {
        chatRoomRepository.save(chatRoom);
        return chatRoom;
    }

    /**
     * 채팅방 리스트를 반환합니다.
     *
     * @param userId
     * @return 채팅방 리스트
     */
    public List<ChatRoomResponse> getChatRooms(Long userId) {
        return chatRoomRepository.findChatRoomListByUserId(userId);
    }

    @Deprecated
    public boolean checkUserExistsInChatRoom(Long chatRoomId, Long userId) {
        Set<ChatRoomUser> chatRoomUsers = chatRoomUserRepository.findById_ChatRoomId(chatRoomId);
        if (chatRoomUsers.isEmpty()) {
            return false;
        }

        //현재 유저가 채팅방에 존재하는지 확인
        boolean currentUserExists = chatRoomUsers.stream()
                .anyMatch(cru -> cru.getUser().getId().equals(userId));
        //상대 유저가 밴된 유저가 아닌지 확인
        boolean otherUserExists = chatRoomUsers.stream()
                .anyMatch(cru -> !cru.getUser().getId().equals(userId) && cru.getUser().getRole() != Role.BANNED);
        return currentUserExists && otherUserExists;
    }
}
