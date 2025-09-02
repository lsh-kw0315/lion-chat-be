package com.lion.be.chat.message.controller;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.chat.message.domain.dto.ChatMessageRequest;
import com.lion.be.chat.message.domain.dto.MessageAckRequest;
import com.lion.be.chat.message.service.MessageUseCase;
import com.lion.be.global.aop.CheckRateLimitChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final MessageUseCase messageUseCase;

    /**
     * 클라이언트로부터 채팅 메시지를 수신해 저장하고, 메시지 발행 후 결과를 반환합니다.
     *
     * @param request 클라이언트가 보낸 채팅 메시지 요청 DTO
     * @return 저장 및 발행된 메시지 정보를 담은 응답 DTO
     */
    @MessageMapping("/chat.sendMessage")
    @CheckRateLimitChat
    public void sendMessageByWebSocket(
            @Payload ChatMessageRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalStateException("Cannot send message without a valid user principal.");
        }

        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        messageUseCase.sendMessage(request, userPrincipal.getId());
    }

    /**
     * 클라이언트로부터 메시지 읽음 확인을 요청합니다.
     * <br>
     * 클라이언트가 특정 메시지를 읽었음을 서버에 알리면, 메시지를 SENT로 갱신합니다.
     *
     * @param ackRequest 읽음 확인 요청 DTO (메시지 ID, 사용자 ID 포함)
     */
    @MessageMapping("/message.ack")
    public void handleMessageAck(MessageAckRequest ackRequest) {
        messageUseCase.processReadAck(ackRequest.messageId(), ackRequest.id());
    }
}
