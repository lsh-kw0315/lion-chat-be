package com.lion.be.feed.service;

import com.lion.be.feed.repository.FeedRepository;
import com.lion.be.feed_comment.repository.FeedCommentRepository;
import com.lion.be.global.util.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedCommentCountScheduler {
    private final RedisTemplate<String, Object> redisTemplate;
    private final FeedRepository feedRepository;


/*
        // 10초마다 실행
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncLikesToDb() {
        long startTime = System.currentTimeMillis();
        log.debug("피드 댓글 카운트 Batch update 시작");

        List<Object> objFeedIds = redisTemplate.opsForSet().pop(RedisKey.DIRTY_COMMENT_COUNT_KEY, 100);

        if (objFeedIds == null || objFeedIds.isEmpty()) {
            log.debug("업데이트 할 피드 댓글 수가 존재하지 않음");
            return;
        }

        for (Object feedIdObj : objFeedIds) {
            String feedIdStr = (String) feedIdObj;
            Long feedId = Long.parseLong(feedIdStr);

            String CommentCountKey = RedisKey.COMMENT_COUNT_KEY + feedId;
            Object CommentCountObj = redisTemplate.opsForValue().get(CommentCountKey);

            if (CommentCountObj != null) {
                long commentCount;
                if (CommentCountObj instanceof Number) {
                    // increment/decrement로 저장된 경우 (Number 타입)
                    commentCount = ((Number) CommentCountObj).longValue();
                } else {
                    // set으로 저장된 경우 (String 타입)
                    commentCount = Long.parseLong(CommentCountObj.toString());
                }

                feedRepository.updateCommentCount(feedId, commentCount);
                log.debug("댓글 수 업데이트 commentId: {}, likeCount: {}", feedId, commentCount);
            }
        }

        log.debug("피드 댓글 수 카운트 Batch update 끝: {}번", objFeedIds.size());
        log.info("피드 댓글 카운트 Batch update 소요 시간: {} ms", System.currentTimeMillis() - startTime);
    }
    */




    // 10초마다 실행
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncLikesToDb() {
        long startTime = System.currentTimeMillis();
        log.debug("피드 댓글 카운트 Batch update 시작");

        // 1. Redis Set에서 처리할 대상 ID를 안전하게 가져옵니다.
        List<Object> objFeedIds = redisTemplate.opsForSet().pop(RedisKey.DIRTY_COMMENT_COUNT_KEY, 100);

        // pop 결과가 null이거나 비어있으면 즉시 종료 (NPE 방지)
        if (objFeedIds == null || objFeedIds.isEmpty()) {
            log.debug("업데이트 할 피드 댓글 수가 존재하지 않음");
            return;
        }

        List<Long> feedIdList = objFeedIds.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .toList();

        // 2. Redis 'MGET'을 사용하여 모든 피드의 좋아요 수를 한 번에 조회합니다.
        List<String> commentCountKeys = feedIdList.stream()
                .map(feedId -> RedisKey.COMMENT_COUNT_KEY + feedId)
                .collect(Collectors.toList());

        List<Object> commentCountObjList = redisTemplate.opsForValue().multiGet(commentCountKeys);

        // multiGet 결과 자체가 null인 예외 케이스 처리
        if (commentCountObjList == null) {
            log.warn("Redis multiGet 결과가 null입니다. 작업을 중단합니다.");
            return;
        }

        // 3. 유효한 데이터(ID와 Count가 모두 존재하는)만 필터링하여 최종 리스트를 생성합니다.
        List<Long> validFeedIds = new ArrayList<>();
        List<Long> validCommentCounts = new ArrayList<>();

        for (int i = 0; i < feedIdList.size(); i++) {
            Object commentCountObj = commentCountObjList.get(i);
            // likeCount가 Redis에 실제로 존재할 경우 (null이 아닐 경우)에만 리스트에 추가
            if (commentCountObj != null) {
                try {
                    validFeedIds.add(feedIdList.get(i));
                    if (commentCountObj instanceof Number) {
                        // increment/decrement로 저장된 경우 (Number 타입)
                        validCommentCounts.add(((Number) commentCountObj).longValue());
                    } else {
                        // set으로 저장된 경우 (String 타입)
                        validCommentCounts.add(Long.parseLong(commentCountObj.toString()));
                    }
                } catch (NumberFormatException e) {
                    log.debug("댓글 수를 Long으로 파싱하는데 실패했습니다. feedId: {}, value: {}",
                            feedIdList.get(i), commentCountObj, e);
                }
            } else {
                // Redis에 해당 피드의 좋아요 카운트 키가 없는 경우. (데이터 불일치 가능성)
                log.warn("피드 ID {}에 해당하는 댓글 수 카운트가 Redis에 존재하지 않습니다.", feedIdList.get(i));
            }
        }

        // 4. 최종적으로 유효한 데이터가 있을 때만 배치 업데이트를 호출합니다.
        if (!validFeedIds.isEmpty()) {
            log.debug("유효한 피드 댓글 수 업데이트 대상: {}개", validFeedIds.size());
            feedRepository.batchUpdateFeedCommentCount(validFeedIds, validCommentCounts);
        } else {
            log.debug("유효한 업데이트 대상이 없습니다.");
        }

        log.debug("피드 댓글 카운트 Batch update 끝");
        log.info("피드 댓글 카운트 Batch update 소요 시간: {} ms", System.currentTimeMillis() - startTime);
    }


}
