package com.lion.be.usercard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserViewHistoryService {

	private final RedisTemplate<String, Object> redisTemplate;
	// Redis key prefix
	private static final String VIEW_HISTORY_KEY_PREFIX = "user:view_history:";

	/**
	 * 현재는 2분 고정
	 * 주석 해제 하여 조회된 유저별 시간들 다르게 지정할수있음
	 * @return TTL적용 시간
	 */
	private Duration getTTL(){
		// int randomTimes = ThreadLocalRandom.current().nextInt(3, 8); // 3-7분 (숫자 조정으로 수정할수있음)
		 int randomTimes = ThreadLocalRandom.current().nextInt(3, 7); // 20 - 40초

		return Duration.ofSeconds(randomTimes);
	}

	/**
	 * 사용자가 본 카드들을 기록합니다. (10분간 유지)
	 *
	 * @param userId 현재 사용자 ID
	 * @param viewedUserIds 본 사용자들의 ID 목록
	 */
	public void recordViewedUsers(Long userId, List<Long> viewedUserIds) {
		if (viewedUserIds == null || viewedUserIds.isEmpty()) {
			return;
		}

		String key = getViewHistoryKey(userId);

		// Redis Set에 추가 (중복 자동 제거)
		String[] userIdStrings = viewedUserIds.stream()
			.map(String::valueOf)
			.toArray(String[]::new);

		redisTemplate.opsForSet().add(key, (Object[]) userIdStrings);

		//TTL 카드별 렌덤 TTL
		redisTemplate.expire(key, getTTL());

	}

	/**
	 * 사용자가 이미 본 사용자 ID들을 조회합니다.
	 *
	 * @param userId 현재 사용자 ID
	 * @return 이미 본 사용자 ID 목록
	 */
	public List<Long> getViewedUserIds(Long userId) {
		String key = getViewHistoryKey(userId);

		Set<Object> viewedIds = redisTemplate.opsForSet().members(key);

		if (viewedIds == null || viewedIds.isEmpty()) {
			return Collections.emptyList();
		}

		return viewedIds.stream()
			.map(id -> Long.valueOf(id.toString()))
			.toList();
	}

	/**
	 * 카드 조회할 때 제외해야 할 모든 사용자 ID들을 반환합니다.
	 * (이미 본 사용자들 + 추가로 제외할 사용자들)
	 *
	 * @param userId 현재 사용자 ID
	 * @param additionalExcludes 추가로 제외할 사용자 ID들
	 * @return 제외해야 할 모든 사용자 ID 목록
	 */
	public List<Long> getExcludeUserIds(Long userId, List<Long> additionalExcludes) {

		// 이미 본 사용자들 추가
		Set<Long> excludeSet = new HashSet<>(getViewedUserIds(userId));

		// 추가로 제외할 사용자들 추가
		if (additionalExcludes != null) {
			excludeSet.addAll(additionalExcludes);
		}

		// 자기 자신도 제외
		excludeSet.add(userId);

		return new ArrayList<>(excludeSet);
	}

	private String getViewHistoryKey(Long userId) {
		return VIEW_HISTORY_KEY_PREFIX + userId;
	}
}
