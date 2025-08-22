package com.lion.be.chat.room.repository;

import com.lion.be.chat.room.domain.entity.ChatRoomUser;
import com.lion.be.chat.room.domain.entity.ChatRoomUserId;
import com.lion.be.global.aop.ElapsedTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, ChatRoomUserId> {
    @ElapsedTime
    @EntityGraph(attributePaths = {"user", "chatRoom"}) //User에 대한 Fetch join을 통해 User 엔티티의 값을 N+1 없이 활용
    @Query("SELECT DISTINCT cru FROM ChatRoomUser cru WHERE cru.id.chatRoomId = :chatRoomId")
    Set<ChatRoomUser> findById_ChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @ElapsedTime
    @Query("""
            SELECT cru.chatRoom.id
            FROM ChatRoomUser cru
            WHERE cru.user.id IN (:userId1, :userId2)
            AND cru.chatRoom.isDeleted = false
            GROUP BY cru.chatRoom.id
            HAVING COUNT(DISTINCT cru.user.id) = 2
            """)
    Optional<Long> findChatRoomIdByTwoUserIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @ElapsedTime
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT cru FROM ChatRoomUser cru WHERE cru.id.chatRoomId = :chatRoomId AND cru.id.userId = :userId")
    ChatRoomUser findById_ChatRoomIdAndId_UserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @ElapsedTime
    boolean existsById_ChatRoomIdAndId_UserId(Long chatRoomId, Long userId);
}
