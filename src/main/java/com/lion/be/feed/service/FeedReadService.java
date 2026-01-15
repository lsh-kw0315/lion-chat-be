package com.lion.be.feed.service;

import com.lion.be.feed.domain.dto.FeedDto;
import com.lion.be.feed.domain.dto.FeedResponse;
import com.lion.be.feed.domain.entity.Feed;
import com.lion.be.feed.domain.entity.FeedLike;
import com.lion.be.feed.repository.FeedLikeRepository;
import com.lion.be.feed.repository.FeedRepository;
import com.lion.be.feed_comment.repository.FeedCommentRepository;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.global.util.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReadService {

    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int DEFAULT_PAGE_SIZE = 30;

    public Feed fetchById(Long id) {
        return feedRepository.findFeed(id)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
    }

    public FeedResponse getFeedResponseById(Long feedId, Long currentUserId) {
        Feed feed = feedRepository.fetchById(feedId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        long likeCount = getAndCacheLikeCount(feed.getId(), feed.getLikeCount());
        boolean isLiked = isLikedByCurrentUser(feedId, currentUserId);
        long commentCount = getAndCacheCommentCount(feed.getId(), feed.getCommentCount());

        return new FeedResponse(
                feed.getId(), feed.getTitle(), feed.getContent(), feed.getCreatedAt(),
                likeCount, isLiked, commentCount,
                feed.getUser().getName(), feed.getUser().getId(), feed.getUser().getImageUrl()
        );
    }

    //===========================

    public Slice<FeedResponse> getRecentFeedsFirst(Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchRecentFeedsFirst(getRecentPageable(size));
        return enrichFeedsWithRedisData(feedResponses, currentUserId);
    }

    public Slice<FeedResponse> getRecentFeedsAfter(Long lastId, Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchRecentFeedsAfter(lastId, getRecentPageable(size));
        return enrichFeedsWithRedisData(feedResponses, currentUserId);
    }


    public Slice<FeedResponse> getHotFeedsFirst(Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchHotFeedsFirst(getHotPageable(size));
        return enrichFeedsWithRedisData(feedResponses, currentUserId);
    }

    public Slice<FeedResponse> getHotFeedsAfter(Long lastLikeCount, Long lastId, Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchHotFeedsAfter(lastLikeCount, lastId, getHotPageable(size));
        return enrichFeedsWithRedisData(feedResponses, currentUserId);
    }

    public Slice<FeedResponse> getMyFeedsAfter(Long currentUserId, Long lastId, Integer size) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchFeedsByUserIdAfter(currentUserId, lastId, getRecentPageable(size));
        return enrichFeedsWithRedisData(feedResponses, currentUserId);
    }

    public Slice<FeedResponse> getMyFeedsFirst(Long currentUserId, Integer size) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchFeedsByUserIdFirst(currentUserId, getRecentPageable(size));
        return enrichFeedsWithRedisData(feedResponses, currentUserId);
    }

    //===================


    public Slice<FeedResponse> getRecentFeedsFirstRDB(Integer size, Long currentUserId) {
        return enrichFeedsWithRDBData(feedRepository.fetchRecentFeedsFirst(getRecentPageable(size)), currentUserId);
    }

    public Slice<FeedResponse> getRecentFeedsAfterRDB(Long lastId, Integer size, Long currentUserId) {
        return enrichFeedsWithRDBData(feedRepository.fetchRecentFeedsAfter(lastId, getRecentPageable(size)),currentUserId);
    }


    public Slice<FeedResponse> getHotFeedsFirstRDB(Integer size, Long currentUserId) {
        return enrichFeedsWithRDBData(feedRepository.fetchHotFeedsFirst(getHotPageable(size)), currentUserId);
    }

    public Slice<FeedResponse> getHotFeedsAfterRDB(Long lastLikeCount, Long lastId, Integer size, Long currentUserId) {
        return enrichFeedsWithRDBData(feedRepository.fetchHotFeedsAfter(lastLikeCount, lastId, getHotPageable(size)), currentUserId);
    }

    public Slice<FeedResponse> getMyFeedsAfterRDB(Long currentUserId, Long lastId, Integer size) {
        return enrichFeedsWithRDBData(feedRepository.fetchFeedsByUserIdAfter(currentUserId, lastId, getRecentPageable(size)), currentUserId);
    }

    public Slice<FeedResponse> getMyFeedsFirstRDB(Long currentUserId, Integer size) {
        return enrichFeedsWithRDBData(feedRepository.fetchFeedsByUserIdFirst(currentUserId, getRecentPageable(size)), currentUserId);
    }

    //=======================

    public Slice<FeedResponse> getRecentFeedsFirstOld(Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchRecentFeedsFirst(getRecentPageable(size));
        return enrichFeedsWithRedisDataOld(feedResponses, currentUserId);
    }


    public Slice<FeedResponse> getRecentFeedsAfterOld(Long lastId, Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchRecentFeedsAfter(lastId, getRecentPageable(size));
        return enrichFeedsWithRedisDataOld(feedResponses, currentUserId);
    }


    public Slice<FeedResponse> getHotFeedsFirstOld(Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchHotFeedsFirst(getHotPageable(size));
        return enrichFeedsWithRedisDataOld(feedResponses, currentUserId);
    }


    public Slice<FeedResponse> getHotFeedsAfterOld(Long lastLikeCount, Long lastId, Integer size, Long currentUserId) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchHotFeedsAfter(lastLikeCount, lastId, getHotPageable(size));
        return enrichFeedsWithRedisDataOld(feedResponses, currentUserId);
    }


    public Slice<FeedResponse> getMyFeedsAfterOld(Long currentUserId, Long lastId, Integer size) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchFeedsByUserIdAfter(currentUserId, lastId, getRecentPageable(size));
        return enrichFeedsWithRedisDataOld(feedResponses, currentUserId);
    }


    public Slice<FeedResponse> getMyFeedsFirstOld(Long currentUserId, Integer size) {
        Slice<FeedResponse> feedResponses = feedRepository.fetchFeedsByUserIdFirst(currentUserId, getRecentPageable(size));
        return enrichFeedsWithRedisDataOld(feedResponses, currentUserId);
    }
    private Slice<FeedResponse> enrichFeedsWithRedisData(Slice<FeedResponse> feeds, Long currentUserId) {

        List<Long> feedIds = feeds.getContent().stream()
                .map(feedResponse -> feedResponse.getFeed().getId())
                .toList();

        List<Object> likeCounts = redisTemplate.opsForValue().multiGet(
                feedIds.stream()
                        .map(id -> RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + id)
                        .collect(Collectors.toList())
        );

        List<Object> commentCounts = redisTemplate.opsForValue().multiGet(
                feedIds.stream()
                        .map(id -> RedisKey.COMMENT_COUNT_KEY + id)
                        .collect(Collectors.toList())
        );

        Set<Object> userLiked = redisTemplate.opsForSet().members(RedisKey.USER_LIKED_FEED_SET_PREFIX+currentUserId);
        Map<Long, Boolean> userLikeMap = new HashMap<>();
        for(Object feedIdObj : userLiked){
            long feedId;
            if(feedIdObj instanceof Number) {
                feedId = ((Number) feedIdObj).longValue();
            }else{
                feedId = Long.parseLong(feedIdObj.toString());
            }
            userLikeMap.put(feedId, true);
        }

        Map<String, Long> feedsToCache = new HashMap<>();

        for(int i=0; i< feeds.getContent().size(); i++) {
            FeedResponse feedResponse = feeds.getContent().get(i);
            FeedDto feedDto = feedResponse.getFeed();
            long feedId = feedDto.getId();

            // ✨ 수정: 캐시 워밍업 로직을 포함한 메서드 호출
            Object likeCountObj = likeCounts.get(i);
            if(likeCountObj != null){
                if(likeCountObj instanceof Number){
                    feedDto.setLikeCount(((Number) likeCountObj).longValue());
                }else{
                    feedDto.setLikeCount(Long.parseLong(likeCountObj.toString()));
                }

            }else{
                feedsToCache.put(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId, feedDto.getLikeCount());
                //redisTemplate.opsForValue().set(RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId, feedDto.getLikeCount());
            }


            Object commentCountObj = commentCounts.get(i);
            if(commentCountObj != null){
                if(commentCountObj instanceof Number){
                    feedDto.setCommentCount(((Number) commentCountObj).longValue());
                }else{
                    feedDto.setCommentCount(Long.parseLong(commentCountObj.toString()));
                }
            }else{
                //redisTemplate.opsForValue().set(RedisKey.COMMENT_COUNT_KEY + feedId, feedDto.getCommentCount());
                feedsToCache.put(RedisKey.COMMENT_COUNT_KEY + feedId, feedDto.getCommentCount());
            }

            if (currentUserId != null) {
                if(userLikeMap.containsKey(feedId)){
                    feedDto.like();
                } else {
                    feedDto.unlike();
                }
            }
        }

        if (!feedsToCache.isEmpty()) {
            redisTemplate.opsForValue().multiSet(feedsToCache);
        }

        return feeds;
    }

    private Slice<FeedResponse> enrichFeedsWithRDBData(Slice<FeedResponse> feeds, Long currentUserId) {

        List<Long> feedIds = feeds.getContent().stream()
                .map(feedResponse -> feedResponse.getFeed().getId())
                .toList();

        List<Object[]> countLikes = feedLikeRepository.countFeeds(feedIds);
        List<Object[]> countComments = feedCommentRepository.countFeeds(feedIds);
        List<FeedLike> userLiked = feedLikeRepository.findAllByFeed_IdInAndUser_Id(feedIds, currentUserId);
        Map<Long, Boolean> likedMap = new HashMap<>();
        Map<Long, Long> feedLikeMap = new HashMap<>();
        Map<Long, Long> feedCommentMap = new HashMap<>();

        for(FeedLike feedLike : userLiked){
            likedMap.put(feedLike.getFeed().getId(), true);
        }

        for(Object[] row : countLikes){
            feedLikeMap.put((Long)row[0], (Long)row[1]);
        }

        for(Object[] row: countComments){
            feedCommentMap.put((Long)row[0], (Long)row[1]);
        }

        for(int i=0; i<feeds.getContent().size(); i++){
            Long feedId = feeds.getContent().get(i).getFeed().getId();
            feeds.getContent().get(i).getFeed().setCommentCount(feedCommentMap.getOrDefault(feedId, 0L));
            feeds.getContent().get(i).getFeed().setLikeCount(feedLikeMap.getOrDefault(feedId, 0L));
            if(likedMap.containsKey(feedId)){
                feeds.getContent().get(i).getFeed().like();
            }
        }

        return feeds;

    }

    private long getAndCacheLikeCount(Long feedId, long dbLikeCount) {
        String likeCountKey = RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId;
        Object likeCountObj = redisTemplate.opsForValue().get(likeCountKey);

        if (likeCountObj != null) {
            return ((Number) likeCountObj).longValue();
        } else {
            redisTemplate.opsForValue().set(likeCountKey, dbLikeCount);
            return dbLikeCount;
        }
    }

    private long getAndCacheCommentCount(Long feedId, long dbCommentCount) {
        String commentCountKey = RedisKey.COMMENT_COUNT_KEY + feedId;
        Object commentCountObj = redisTemplate.opsForValue().get(commentCountKey);

        if (commentCountObj != null) {
            return ((Number) commentCountObj).longValue();
        } else {
            redisTemplate.opsForValue().set(commentCountKey, dbCommentCount);
            return dbCommentCount;
        }
    }

    private boolean isLikedByCurrentUser(Long feedId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId, String.valueOf(currentUserId)));
    }


    private Slice<FeedResponse> enrichFeedsWithRedisDataOld(Slice<FeedResponse> feeds, Long currentUserId) {
        feeds.getContent().forEach(feedResponse -> {
            FeedDto feedDto = feedResponse.getFeed();
            long feedId = feedDto.getId();

            // ✨ 수정: 캐시 워밍업 로직을 포함한 메서드 호출
            long finalLikeCount = getAndCacheLikeCountOld(feedId, feedDto.getLikeCount());
            feedDto.setLikeCount(finalLikeCount);

            long finalCommentCount = getAndCacheCommentCountOld(feedId, feedDto.getCommentCount());
            feedDto.setCommentCount(finalCommentCount);

            if (currentUserId != null) {
                if(isLikedByCurrentUserOld(feedId, currentUserId)){
                    feedDto.like();
                } else {
                    feedDto.unlike();
                }
            }
        });
        return feeds;
    }

    private long getAndCacheLikeCountOld(Long feedId, long dbLikeCount) {
        String likeCountKey = RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId;
        Object likeCountObj = redisTemplate.opsForValue().get(likeCountKey);

        if (likeCountObj != null) {
            return ((Number) likeCountObj).longValue();
        } else {
            redisTemplate.opsForValue().set(likeCountKey, dbLikeCount);
            return dbLikeCount;
        }
    }

    private long getAndCacheCommentCountOld(Long feedId, long dbCommentCount) {
        String commentCountKey = RedisKey.COMMENT_COUNT_KEY + feedId;
        Object commentCountObj = redisTemplate.opsForValue().get(commentCountKey);

        if (commentCountObj != null) {
            return ((Number) commentCountObj).longValue();
        } else {
            redisTemplate.opsForValue().set(commentCountKey, dbCommentCount);
            return dbCommentCount;
        }
    }

    private boolean isLikedByCurrentUserOld(Long feedId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(RedisKey.FEED_LIKED_USERS_KEY_PREFIX + feedId, String.valueOf(currentUserId)));
    }

    private Pageable getRecentPageable(Integer size) {
        return PageRequest.of(0,
                size != null && size <= DEFAULT_PAGE_SIZE && size > 0 ? size : DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id"));
    }

    private Pageable getHotPageable(Integer size) {
        return PageRequest.of(0,
                size != null && size <= DEFAULT_PAGE_SIZE && size > 0 ? size : DEFAULT_PAGE_SIZE);
    }

}