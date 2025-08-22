package com.lion.be.chat.message.controller;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.chat.message.domain.dto.ChatMessageResponse;
import com.lion.be.chat.message.service.MessageUseCase;
import com.lion.be.global.aop.ElapsedTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final MessageUseCase messageUseCase;

    /**
     * 채팅방의 이전 메시지 내역을 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @param lastId 마지막으로 조회한 메시지 ID
     * @return 이전 메시지 목록
     */
    @PreAuthorize("@chatRoomUserRepository.existsById_ChatRoomIdAndId_UserId(#roomId, #userPrincipal.id)")
    @GetMapping("/messages")
    @ElapsedTime
    public ResponseEntity<List<ChatMessageResponse>> getMessageHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long roomId,
            @RequestParam(required = false) String lastId
    ) {
        List<ChatMessageResponse> messages = messageUseCase.findMessagesByIdAndLastId(roomId, lastId, userPrincipal.getId());
        return ResponseEntity.ok(messages);
    }
}
