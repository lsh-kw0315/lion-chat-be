package com.lion.be.chat.room.repository;

import com.lion.be.chat.room.domain.dto.ChatRoomResponse;
import com.lion.be.chat.room.domain.entity.QChatRoom;
import com.lion.be.chat.room.domain.entity.QChatRoomUser;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.image.domain.entity.QImage;
import com.lion.be.user.domain.entity.QUser;
import com.lion.be.user.domain.entity.QUserPhoto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatRoomQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @ElapsedTime
    public List<ChatRoomResponse> findChatRoomResponsesByUserIdQueryDsl(Long userId) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QChatRoomUser currentUserRoom = new QChatRoomUser("currentUserRoom");
        QChatRoomUser otherUserRoom = new QChatRoomUser("otherUserRoom");
        QUser otherUser = QUser.user;
        QUserPhoto otherUserPhoto = QUserPhoto.userPhoto;
        QImage otherUserImage = QImage.image;

        return queryFactory
                .select(Projections.constructor(ChatRoomResponse.class,
                        chatRoom.id,
                        otherUser.nickname,
                        chatRoom.recentMessageContent,
                        chatRoom.recentMessageDt,
                        otherUserImage.imageUrl.coalesce("https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png"),
                        currentUserRoom.isRead.coalesce(true)
                ))
                .from(chatRoom)
                .join(chatRoom.chatRoomUsers, currentUserRoom)
                .join(chatRoom.chatRoomUsers, otherUserRoom)
                .join(otherUserRoom.user, otherUser)
                .leftJoin(otherUser.userPhotos, otherUserPhoto)
                .on(otherUserPhoto.orderIndex.eq(1))
                .leftJoin(otherUserPhoto.image, otherUserImage)
                .where(
                        currentUserRoom.user.id.eq(userId)
                                .and(otherUserRoom.user.id.ne(userId))
                                .and(chatRoom.isDeleted.isFalse())
                )
                .orderBy(chatRoom.recentMessageDt.desc().nullsLast())
                .fetch();
    }
}
