package com.lion.be.feed_comment.service;

import com.lion.be.feed_comment.domain.dto.FeedCommentResponse;
import com.lion.be.feed_comment.repository.FeedCommentRepository;


import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedCommentReadService {

    private final FeedCommentRepository feedCommentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LIKE_COUNT_KEY_PREFIX = "comment:like_count:";
    private static final String LIKED_USERS_KEY_PREFIX = "comment:liked_users:";

    @ElapsedTime
    public Slice<FeedCommentResponse> fetchAll(Long feedId, Long lastId, int size, Long userId) {
        Pageable pageable = PageRequest.of(0, size > 0 && size <=30 ? size:30);

        Slice<FeedCommentResponse> slice;
        if(lastId != null) {
            slice = feedCommentRepository.fetchAllByFeedIdAfter(feedId, lastId, pageable);
        }else{
            slice = feedCommentRepository.fetchAllByFeedIdFirst(feedId, pageable);
        }

        return slice;
    }


    public FeedCommentResponse fetchById(Long commentId, Long id) {
        FeedCommentResponse comment = feedCommentRepository.findCommentById(commentId);

        if (comment == null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUNT);
        }

        String likeCountKey = LIKE_COUNT_KEY_PREFIX + comment.id();
        String likedUsersKey = LIKED_USERS_KEY_PREFIX + comment.id();

        Object likeCountObj = redisTemplate.opsForValue().get(likeCountKey);
        long finalLikeCount;

        if (likeCountObj != null) {
            finalLikeCount = ((Number) likeCountObj).longValue();
        } else {
            finalLikeCount = comment.likeCount(); // DB에서 조회한 likeCount 사용
            redisTemplate.opsForValue().set(likeCountKey, finalLikeCount);
        }

        boolean isLiked = Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(likedUsersKey, String.valueOf(id))
        );

        return new FeedCommentResponse(
                comment.id(),
                comment.feedId(),
                comment.writer(),
                comment.content(),
                finalLikeCount, // 최종 계산된 좋아요 수
                isLiked,
                comment.createdAt()
        );
    }
}
