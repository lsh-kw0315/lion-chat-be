package com.lion.be.chat.room.domain.dto;

public record ChatRoomInitResponse(
        Long chatRoomId
) {
    public static ChatRoomInitResponse toResponse(
            Long chatRoomId
    ) {
        return new ChatRoomInitResponse(
                chatRoomId
        );
    }
}
