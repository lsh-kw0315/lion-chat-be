package com.lion.be.feed.service;

import com.lion.be.feed.repository.FeedRepository;

import java.util.*;
import java.util.stream.Collectors;

import com.lion.be.global.util.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedLikeScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FeedRepository feedRepository;


    /*

        @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncLikesToDb() {
        log.debug("피드 좋아요 카운트 Batch update 시작");

        List<Object> objFeedIds = redisTemplate.opsForSet().pop(RedisKey.DIRTY_FEED_LIKE_KEY, 100);

        if (objFeedIds == null || objFeedIds.isEmpty()) {
            log.debug("업데이트 할 피드 좋아요가 존재하지 않음");
            return;
        }

        for (Object feedIdObj : objFeedIds) {
            String feedIdStr = (String) feedIdObj;
            Long feedId = Long.parseLong(feedIdStr);
            String likeCountKey = RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId;
            Object likeCountObj = redisTemplate.opsForValue().get(likeCountKey);

            if (likeCountObj != null) {
                long likeCount;
                if (likeCountObj instanceof Number) {
                    likeCount = ((Number) likeCountObj).longValue();
                } else {
                    likeCount = Long.parseLong(likeCountObj.toString());
                }
                feedRepository.updateLikeCount(feedId, likeCount);
                log.debug("업데이트 feedId: {}, likeCount: {}", feedId, likeCount);
            }
        }

        log.debug("피드 좋아요 카운트 Batch update 끝: {}번", objFeedIds.size());
    }

    */

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncLikesToDb() {
        long startTime = System.currentTimeMillis();

        log.debug("피드 좋아요 카운트 Batch update 시작");

        // 1. Redis Set에서 처리할 대상 ID를 안전하게 가져옵니다.
        List<Object> objFeedIds = redisTemplate.opsForSet().pop(RedisKey.DIRTY_FEED_LIKE_KEY, 100);

        // pop 결과가 null이거나 비어있으면 즉시 종료 (NPE 방지)
        if (objFeedIds == null || objFeedIds.isEmpty()) {
            log.debug("업데이트 할 피드 좋아요가 존재하지 않음");
            return;
        }

        List<Long> feedIdList = objFeedIds.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .toList();

        // 2. Redis 'MGET'을 사용하여 모든 피드의 좋아요 수를 한 번에 조회합니다.
        List<String> likeCountKeys = feedIdList.stream()
                .map(feedId -> RedisKey.FEED_LIKE_COUNT_KEY_PREFIX + feedId)
                .collect(Collectors.toList());

        List<Object> likeCountObjList = redisTemplate.opsForValue().multiGet(likeCountKeys);

        // multiGet 결과 자체가 null인 예외 케이스 처리
        if (likeCountObjList == null) {
            log.warn("Redis multiGet 결과가 null입니다. 작업을 중단합니다.");
            return;
        }

        // 3. 유효한 데이터(ID와 Count가 모두 존재하는)만 필터링하여 최종 리스트를 생성합니다.
        List<Long> validFeedIds = new ArrayList<>();
        List<Long> validLikeCounts = new ArrayList<>();

        for (int i = 0; i < feedIdList.size(); i++) {
            Object likeCountObj = likeCountObjList.get(i);
            // likeCount가 Redis에 실제로 존재할 경우 (null이 아닐 경우)에만 리스트에 추가
            if (likeCountObj != null) {
                try {
                    validFeedIds.add(feedIdList.get(i));
                    if (likeCountObj instanceof Number) {
                        // increment/decrement로 저장된 경우 (Number 타입)
                        validLikeCounts.add(((Number) likeCountObj).longValue());
                    } else {
                        // set으로 저장된 경우 (String 타입)
                        validLikeCounts.add(Long.parseLong(likeCountObj.toString()));
                    }
                } catch (NumberFormatException e) {
                    log.debug("좋아요 수(likeCount)를 Long으로 파싱하는데 실패했습니다. feedId: {}, value: {}",
                            feedIdList.get(i), likeCountObj, e);
                }
            } else {
                // Redis에 해당 피드의 좋아요 카운트 키가 없는 경우. (데이터 불일치 가능성)
                log.warn("피드 ID {}에 해당하는 좋아요 카운트가 Redis에 존재하지 않습니다.", feedIdList.get(i));
            }
        }

        // 4. 최종적으로 유효한 데이터가 있을 때만 배치 업데이트를 호출합니다.
        if (!validFeedIds.isEmpty()) {
            log.debug("유효한 피드 좋아요 업데이트 대상: {}개", validFeedIds.size());
            feedRepository.batchUpdateFeedLikeCount(validFeedIds, validLikeCounts);
        } else {
            log.debug("유효한 업데이트 대상이 없습니다.");
        }

        log.debug("피드 좋아요 카운트 Batch update 끝");
        log.info("피드 좋아요 카운트 Batch update 소요 시간: {} ms", System.currentTimeMillis() - startTime);
    }

}
