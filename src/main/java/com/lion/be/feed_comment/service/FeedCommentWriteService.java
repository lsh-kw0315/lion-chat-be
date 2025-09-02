package com.lion.be.feed_comment.service;

import com.lion.be.feed.domain.entity.Feed;
import com.lion.be.feed.service.FeedReadService;
import com.lion.be.feed_comment.domain.dto.FeedCommentResponse;
import com.lion.be.feed_comment.domain.dto.FeedCommentSaveRequest;
import com.lion.be.feed_comment.domain.dto.FeedCommentSaveResponse;
import com.lion.be.feed_comment.domain.dto.FeedCommentUpdateRequest;
import com.lion.be.feed_comment.domain.dto.FeedCommentUpdateResponse;
import com.lion.be.feed_comment.domain.entity.FeedComment;
import com.lion.be.feed_comment.repository.FeedCommentRepository;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.global.util.RedisKey;
import com.lion.be.notification.domain.NotificationType;
import com.lion.be.notification.domain.dto.NotificationEvent;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.service.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedCommentWriteService {

    private final FeedCommentRepository feedCommentRepository;
    private final FeedReadService feedReadService;
    private final UserReadService userReadService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationRepository notificationRepository;

    public FeedCommentSaveResponse save(Long feedId, Long userId, FeedCommentSaveRequest request) {
        Feed feed = feedReadService.fetchById(feedId);
        User user = userReadService.fetchById(userId);

        FeedComment newComment = FeedComment.of(feed, user, request.getContent());
        FeedCommentSaveResponse savedResponse = feedCommentRepository.save(newComment);

        // 피드 댓글 저장 후, 댓글 ID를 사용하여 FeedComment 객체를 업데이트
        feed.addComment(newComment);

        String commentCountKey = RedisKey.COMMENT_COUNT_KEY + feedId;
        redisTemplate.opsForValue().increment(commentCountKey);
        redisTemplate.opsForSet().add(RedisKey.DIRTY_COMMENT_COUNT_KEY, String.valueOf(feedId));

        Long feedWriterId = feed.getUser().getId();
        if(!feedWriterId.equals(userId)) {

            applicationEventPublisher.publishEvent(
                    new NotificationEvent(userId, feedWriterId, NotificationType.COMMENT, feed.getId())
            );
        }

        return new FeedCommentSaveResponse(savedResponse.commentId());
    }

    public FeedCommentUpdateResponse update(Long commentId, Long userId, FeedCommentUpdateRequest request) {
        FeedComment feedComment = feedCommentRepository.fetchById(commentId);

        if (!feedComment.getUser().getId().equals(userId)) {
            throw new RuntimeException("TODO: 입력한 유저가 아닙니다.");
        }

        feedComment.updateContent(request.getContent());
        return new FeedCommentUpdateResponse(feedComment.getId());
    }

    public void delete(Long commentId, Long requestedUserId) {
        FeedCommentResponse feedComment = feedCommentRepository.findCommentById(commentId);
        if(feedComment == null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUNT);
        }

        // 피드 존재 여부 확인
        Long feedId = feedComment.feedId();
        feedReadService.fetchById(feedId);

        User user = userReadService.fetchById(requestedUserId);

        // 댓글 작성자와 요청한 사용자가 일치하는지 확인
        Long writerId = feedComment.writer().id();
        if(!requestedUserId.equals(writerId) && !user.getRole().equals(Role.ADMIN)) {
            throw new CustomException(ErrorCode.USER_UNAUTHORIZED);
        }

        String commentCountKey = RedisKey.COMMENT_COUNT_KEY + feedId;
        Long currentCommentCount = Long.parseLong(redisTemplate.opsForValue().get(commentCountKey).toString());

        if (currentCommentCount != null && currentCommentCount > 0) {
            redisTemplate.opsForValue().decrement(commentCountKey);
            redisTemplate.opsForSet().add(RedisKey.DIRTY_COMMENT_COUNT_KEY, String.valueOf(feedId));
        }

        feedCommentRepository.deleteById(commentId);
    }

    public void deleteAllByFeedId(Long feedId) {
        // 피드 삭제 시 댓글 전부 삭제 처리
        feedCommentRepository.softDeleteByFeedId(feedId);
    }

}
