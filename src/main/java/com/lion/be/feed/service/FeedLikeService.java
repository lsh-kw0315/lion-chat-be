package com.lion.be.feed.service;

import java.util.Objects;
import java.util.Optional;

import com.lion.be.feed.domain.entity.Feed;
import com.lion.be.feed.domain.entity.FeedLike;
import com.lion.be.feed.repository.FeedLikeRepository;
import com.lion.be.feed.repository.FeedRepository;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.global.util.RedisKey;
import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.dto.PostLikeEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedLikeService {

    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likeFeedRDB(Long feedId, Long userId){
        String likedUsersKey = RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId;
        String userIdStr = String.valueOf(userId);

        Feed feed = feedRepository.findFeed(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
        Long writerId = feed.getUser().getId();

        if (Objects.equals(redisTemplate.opsForSet().add(likedUsersKey, userIdStr), 1L)) {
            redisTemplate.opsForValue().increment(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_FEED_LIKE_KEY, String.valueOf(feedId));
            redisTemplate.opsForSet().add(RedisKey.USER_LIKED_FEED_SET_PREFIX + userIdStr, String.valueOf(feedId));
            Optional<FeedLike> feedLikeOptional = feedLikeRepository.findFeedLikeByUser_IdAndFeed_IdAndIsDeletedFalse(userId, feedId);
            if(feedLikeOptional.isEmpty()){
                User likedUser = userRepository.fetchById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                FeedLike feedLike = new FeedLike(feed,likedUser);
                feedLikeRepository.save(feedLike);
            }else{
                feedLikeOptional.get().relike();
            }


            if(!writerId.equals(userId)) {
                applicationEventPublisher.publishEvent(
                        new PostLikeEvent(userId, writerId, NotificationType.POST_LIKE, feedId)
                );
            }
        }
    }

    @Transactional
    public void unlikeFeedRDB(Long feedId, Long userId){
        String likedUsersKey = RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId;
        String userIdStr = String.valueOf(userId);

        if (Objects.equals(redisTemplate.opsForSet().remove(likedUsersKey, userIdStr), 1L)) {
            FeedLike feedLike = feedLikeRepository.findFeedLikeByUser_IdAndFeed_IdAndIsDeletedFalse(userId, feedId).orElseThrow(()->new CustomException(ErrorCode.SERVER_ERROR));
            feedLike.unlike();
            redisTemplate.opsForValue().decrement(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_FEED_LIKE_KEY, String.valueOf(feedId));
            redisTemplate.opsForSet().remove(RedisKey.USER_LIKED_FEED_SET_PREFIX + userIdStr, String.valueOf(feedId));
        }
    }

    public void likeFeed(Long feedId, Long userId) {

        String likedUsersKey = RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId;
        String userIdStr = String.valueOf(userId);

        Feed feed = feedRepository.findFeed(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
        Long writerId = feed.getUser().getId();


        if (Objects.equals(redisTemplate.opsForSet().add(likedUsersKey, userIdStr), 1L)) {
            redisTemplate.opsForValue().increment(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_FEED_LIKE_KEY, String.valueOf(feedId));
            redisTemplate.opsForSet().add(RedisKey.USER_LIKED_FEED_SET_PREFIX + userIdStr, String.valueOf(feedId));

            Optional<FeedLike> feedLikeOptional = feedLikeRepository.findFeedLikeByUser_IdAndFeed_IdAndIsDeletedFalse(userId, feedId);
            if(feedLikeOptional.isEmpty()){
                User likedUser = userRepository.fetchById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                FeedLike feedLike = new FeedLike(feed,likedUser);
                feedLikeRepository.save(feedLike);
            }else{
                feedLikeOptional.get().relike();
            }


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
            FeedLike feedLike = feedLikeRepository.findFeedLikeByUser_IdAndFeed_IdAndIsDeletedFalse(userId, feedId).orElseThrow(()->new CustomException(ErrorCode.SERVER_ERROR));
            feedLike.unlike();
            redisTemplate.opsForValue().decrement(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_FEED_LIKE_KEY, String.valueOf(feedId));
            redisTemplate.opsForSet().remove(RedisKey.USER_LIKED_FEED_SET_PREFIX + userIdStr, String.valueOf(feedId));
        }
    }

}
