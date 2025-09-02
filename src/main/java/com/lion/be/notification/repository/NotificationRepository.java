package com.lion.be.notification.repository;

import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n.toUserId
        FROM Notification n
        WHERE n.type =  'PROFILE_LIKE'
        AND n.fromUserId = :currentUserId
        AND n.toUserId IN :targetUserIds
    """) //카드 내에서 좋아요 한 유무체크
    List<Long> findLikedUserIdsAmon(
            @Param("currentUserId") Long currentUserId,
            @Param("targetUserIds") List<Long> targetUserIds
    );

    @Query("""
select n from Notification n
where n.fromUserId = :currentUserId and n.toUserId = :targetUserId and n.type = 'PROFILE_LIKE'
""")
    Optional<Notification> extractProfileLike(@Param("currentUserId")Long currentUserId, @Param("targetUserId")Long targetUserId);

    @Modifying(clearAutomatically = true)
    @Query("""
delete from Notification n where n.fromUserId = :currentUserId and n.toUserId = :targetUserId and n.type = 'PROFILE_LIKE'
""")
    void deleteNotification(@Param("currentUserId")Long currentUserId, @Param("targetUserId")Long targetUserId);

	@Query("""
    SELECT DISTINCT u FROM User u
    WHERE u.id IN (
        SELECT n.toUserId FROM Notification n
        WHERE n.fromUserId = :userId and n.type = 'PROFILE_LIKE'
    )
    ORDER BY u.id
""")
	Page<User> fetchAllLikeUser(@Param("userId") Long userId, Pageable pageable);

    @ElapsedTime
    @Query("""
    select n
    from Notification n
    where n.toUserId = :userId
""")
    List<Notification> fetchAllFirst(@Param("userId") Long userId, Pageable pageable);

    @ElapsedTime
    @Query("""
    select n
    from Notification n
    where n.toUserId = :userId and n.id < :lastId
""")
    List<Notification> fetchAllSecond(@Param("userId") Long userId, @Param("lastId") Long lastId, Pageable pageable);

}
