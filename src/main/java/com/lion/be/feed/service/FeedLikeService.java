package com.lion.be.feed.service;

import java.util.Objects;
import java.util.Optional;

import com.lion.be.feed.domain.entity.Feed;
import com.lion.be.feed.repository.FeedRepository;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.global.util.RedisKey;
import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.dto.PostLikeEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedLikeService {

    private final FeedRepository feedRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationRepository notificationRepository;

    public void likeFeed(Long feedId, Long userId) {

        String likedUsersKey = RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId;
        String userIdStr = String.valueOf(userId);

        Feed feed = feedRepository.findFeed(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
        Long writerId = feed.getUser().getId();

        if (Objects.equals(redisTemplate.opsForSet().add(likedUsersKey, userIdStr), 1L)) {
            redisTemplate.opsForValue().increment(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_FEED_LIKE_KEY, String.valueOf(feedId));
            redisTemplate.opsForSet().add(RedisKey.USER_LIKED_FEED_SET_PREFIX + userIdStr, String.valueOf(feedId));

            if(!writerId.equals(userId)) {
                applicationEventPublisher.publishEvent(
                        new PostLikeEvent(userId, writerId, NotificationType.POST_LIKE, feedId)
                );
            }
        }
    }

    public void unlikeFeed(Long feedId, Long userId) {
        String likedUsersKey = RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId;
        String userIdStr = String.valueOf(userId);

        if (Objects.equals(redisTemplate.opsForSet().remove(likedUsersKey, userIdStr), 1L)) {
            redisTemplate.opsForValue().decrement(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_FEED_LIKE_KEY, String.valueOf(feedId));
            redisTemplate.opsForSet().remove(RedisKey.USER_LIKED_FEED_SET_PREFIX + userIdStr, String.valueOf(feedId));
        }
    }

}
