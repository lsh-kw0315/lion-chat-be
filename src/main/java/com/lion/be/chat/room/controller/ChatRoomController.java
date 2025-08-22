package com.lion.be.chat.room.controller;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.chat.room.domain.dto.ChatRoomParticipantsInfoResponse;
import com.lion.be.chat.room.domain.dto.ChatRoomInitRequest;
import com.lion.be.chat.room.domain.dto.ChatRoomInitResponse;
import com.lion.be.chat.room.domain.dto.ChatRoomResponse;
import com.lion.be.chat.room.service.ChatRoomService;
import com.lion.be.global.aop.ElapsedTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 새로운 채팅방을 생성합니다.
     *
     * @param userPrincipal
     * @param request
     * @return 생성한 채팅방 id
     */
    @PostMapping("/init")
    @ElapsedTime
    public ResponseEntity<ChatRoomInitResponse> createChatRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ChatRoomInitRequest request
    ) {
        Long chatRoomId = chatRoomService.findOrCreateChatRoom(userPrincipal.getId(), request.id());
        log.info("채팅방 생성 완료: {}", chatRoomId);
        return ResponseEntity.ok(new ChatRoomInitResponse(
                chatRoomId
        ));
    }

    /**
     * 사용자의 채팅방을 조회합니다.
     * @param userPrincipal
     * @return 채팅방 목록 일괄조회
     */
    @GetMapping
    @ElapsedTime
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRoomList(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(chatRoomService.getChatRooms(userPrincipal.getId()));
    }

    /**
     * 채팅방 참여자의 정보를 불러옵니다.
     * @param userPrincipal
     * @param roomId
     * @return
     */
    @GetMapping("/context")
    @ElapsedTime
    public ResponseEntity<ChatRoomParticipantsInfoResponse> getChatRoomParticipants(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long roomId
    ) {
        return ResponseEntity.ok(chatRoomService.findChatRoomParticipants(userPrincipal.getId(), roomId));
    }
}
