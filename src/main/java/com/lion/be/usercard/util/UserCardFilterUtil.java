package com.lion.be.usercard.util;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.user.domain.Mbti;
import com.lion.be.user.domain.Position;
import com.lion.be.user.domain.PreferenceType;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.domain.entity.UserVectorizer;
import com.lion.be.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 카드 필터링 및 클러스터 기반 추천 시스템
 *
 * 주요 개선사항:
 * - 신규 사용자 가입 시 전체 재클러스터링 → 사전 정의된 클러스터 맵으로 즉시 배정
 * - 1600명 동시 가입 시에도 안정적 성능 보장
 * - 기존 사용자 클러스터 변경 방지
 *
 * 핵심 로직:
 * 1. UserVectorizer의 9차원 벡터 로직을 활용한 클러스터 맵 생성
 * 2. MBTI(16) × Position(5) = 80가지 조합을 6개 클러스터로 분류
 * 3. 코사인 유사도 기반 클러스터 중심점 매칭
 *
 * 메서드 호출 순서
 * 1.getRecommendedUsers
 * 2.getClusterBasedRecommendations (여기서 클러스터 사용자 조회 및 유사도 계산 (calculateSimilarity메서드 사용))
 *
 */
@Slf4j
@Component
public class UserCardFilterUtil {

	private final UserVectorizer userVectorizer;
	private final UserRepository userRepository;

	/**
	 * 벡터 기반 사전 정의된 클러스터 맵
	 * Key: "MBTI_Position" (예: "INTJ_BACKEND")
	 * Value: 클러스터 ID (0~5)
	 *
	 * 애플리케이션 시작 시 한 번만 생성되어 메모리에 상주하며
	 * 신규 사용자 클러스터 배정 시 O(1) HashMap 조회로 즉시 반환
	 */
	private static final Map<String, Integer> VECTOR_BASED_CLUSTER_MAP = createVectorBasedClusterMap();

	public UserCardFilterUtil(UserVectorizer userVectorizer, UserRepository userRepository) {
		this.userVectorizer = userVectorizer;
		this.userRepository = userRepository;
	}

	/**
	 * 클러스터 기반 사용자 추천 메인 메서드
	 *
	 * 동작 순서:
	 * 1. 동일 클러스터 내 사용자 조회 및 유사도 기반 정렬
	 * 2. 부족한 경우 랜덤 사용자로 보완
	 *
	 * @param userId 추천 대상 사용자 ID
	 * @param size 추천할 사용자 수
	 * @param excludeUserIds 제외할 사용자 ID 목록
	 * @return 추천 사용자 목록
	 */
	public List<User> getRecommendedUsers(Long userId, int size, List<Long> excludeUserIds) {
		if (size <= 0) {
			log.warn("잘못된 size 값: {}", size);
			return new ArrayList<>();
		}

		User targetUser = userRepository.fetchById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int clusterSize = 7; // 클러스터 내 추천 사용자 수
        int recentSize =3; // 최근 가입자 추천 사용자 수

		// 1단계: 동일 클러스터 기반 추천
		List<User> clusterBasedUsers = new ArrayList<>(getClusterBasedRecommendations(targetUser, excludeUserIds, clusterSize));

        // 최근 가입자 우선 추천
        List<Long> extendedExcludeIds = new ArrayList<>(excludeUserIds != null ? excludeUserIds : List.of());
        clusterBasedUsers.forEach(user -> extendedExcludeIds.add(user.getId()));

        List<User> recentUsers = userRepository.fetchRandomUsersExcluding(
                userId, recentSize, extendedExcludeIds);

        List<User> allUsers = new ArrayList<>(clusterBasedUsers);
        allUsers.addAll(recentUsers);

		// 2단계: 부족한 경우 랜덤 사용자로 보완
		if (allUsers.size() < size) {
            int remainingSize = size - allUsers.size();

            List<Long> finalExcludeIds  = new ArrayList<>(excludeUserIds != null ? excludeUserIds : List.of());
            allUsers.forEach(user -> finalExcludeIds.add(user.getId()));

            List<User> additionalUsers = userRepository.fetchRandomUsersExcluding(
                    userId, remainingSize, finalExcludeIds);
            allUsers.addAll(additionalUsers);
		}

        // 시간 기반 셔플 (10초 다른 순서)
        long timeSeed = System.currentTimeMillis() / (10 * 1000);
        Random random = new Random(timeSeed);
        Collections.shuffle(allUsers, random);

        return allUsers.stream().limit(size).collect(Collectors.toList());
	}

	/**
	 * 동일 클러스터 내 사용자 추천 및 유사도 기반 정렬
	 *
	 * @param targetUser 기준 사용자
	 * @param excludeUserIds 제외할 사용자 ID 목록
	 * @param size 추천할 사용자 수
	 * @return 유사도 순으로 정렬된 사용자 목록
	 */
	private List<User> getClusterBasedRecommendations(User targetUser, List<Long> excludeUserIds, int size) {
		Integer targetClusterId = targetUser.getClusterId();

		if (targetClusterId == null) {
			log.warn("사용자 {}의 클러스터 정보 없음", targetUser.getId());
			return new ArrayList<>();
		}

		if (size <= 0) {
			return new ArrayList<>();
		}

        int candidateSize = size * 3; // 후보군 3배수 조회 (30)
		// 동일 클러스터 사용자 조회
		List<User> sameClusterUsers = userRepository.fetchUsersByClusterExcluding(
			targetClusterId, targetUser.getId(), excludeUserIds, candidateSize);

		if (sameClusterUsers.isEmpty()) {
			return new ArrayList<>();
		}

		// 유사도 계산 후 내림차순 정렬
		return sameClusterUsers.stream()
			.map(user -> new UserSimilarity(user, calculateSimilarity(targetUser, user)))
			.sorted((a, b) -> Double.compare(b.similarity, a.similarity))
			.limit(size)
			.map(us -> us.user)
                .collect(Collectors.toList());
	}

	/**
	 * 두 사용자 간 종합 유사도 계산
	 *
	 * 사용자의 선호도에 따라 MBTI와 Position 유사도를 가중 평균
	 * - MBTI_FOCUSED: MBTI 70% + Position 30%
	 * - POSITION_FOCUSED: MBTI 30% + Position 70%
	 * - BOTH_FOCUSED: MBTI 50% + Position 50%
	 *
	 * @param user1 첫 번째 사용자
	 * @param user2 두 번째 사용자
	 * @return 0.0~1.0 범위의 유사도 점수
	 */
	private double calculateSimilarity(User user1, User user2) {
		double mbtiCompatibility = userVectorizer.calculateMbtiCompatibility(user1, user2);
		double positionSimilarity = calculatePositionSimilarity(user1, user2);

		PreferenceType preference = user1.getPreferenceType();

		if (preference == PreferenceType.PREFERENCE_FOCUSED) {
			return 0.7 * mbtiCompatibility + 0.3 * positionSimilarity;
		} else if (preference == PreferenceType.POSITION_FOCUSED) {
			return 0.3 * mbtiCompatibility + 0.7 * positionSimilarity;
		} else if (preference == PreferenceType.CAREER_FOCUSED){
			return 0.5 * mbtiCompatibility + 0.5 * positionSimilarity;
		} else {
			throw new CustomException(ErrorCode.INVALID_PREFERENCE_TYPE);
		}
	}

	/**
	 * 두 사용자의 Position 벡터 간 유사도 계산
	 *
	 * UserVectorizer.getPositionVector()로 생성된 5차원 벡터의 내적(dot product) 계산
	 *
	 * 예시:
	 * - BACKEND vs BACKEND: 1.0 (동일)
	 * - BACKEND vs FULLSTACK: 0.8 (높은 유사도)
	 * - BACKEND vs PM: 0.2 (낮은 유사도)
	 *
	 * @param user1 첫 번째 사용자
	 * @param user2 두 번째 사용자
	 * @return Position 유사도 점수
	 */
	private double calculatePositionSimilarity(User user1, User user2) {
		double[] vector1 = userVectorizer.getPositionVector(user1.getPosition());
		double[] vector2 = userVectorizer.getPositionVector(user2.getPosition());

		double similarity = 0.0;
		for (int i = 0; i < vector1.length; i++) {
			similarity += vector1[i] * vector2[i];
		}

		return similarity;
	}

	public List<User> getRecommendedUsersByPosition(Long userId, int size, List<Long> excludeUserIds, Position filterPosition) {
		if (size <= 0) {
			log.warn("잘못된 size 값: {}", size);
			return new ArrayList<>();
		}

		User targetUser = userRepository.fetchById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		int clusterSize = 2;
		int recentSize = 8;

		// 1단계: 클러스터 기반 추천
		List<User> clusterBasedUsers = getClusterBasedRecommendations(targetUser, excludeUserIds, clusterSize * 3).stream()
			.filter(user -> user.getPosition() == filterPosition)
			.limit(clusterSize)
			.collect(Collectors.toCollection(ArrayList::new));

		List<Long> extendedExcludeIds = new ArrayList<>(excludeUserIds != null ? excludeUserIds : List.of());
		clusterBasedUsers.forEach(user -> extendedExcludeIds.add(user.getId()));

		List<User> recentUsers = userRepository.fetchRandomUsersByPositionExcluding(
			userId, filterPosition, recentSize, extendedExcludeIds);

		List<User> allUsers = new ArrayList<>(clusterBasedUsers);
		allUsers.addAll(recentUsers);

		// 3단계: 부족하면 해당 Position의 랜덤 사용자로 보완
		if (allUsers.size() < size) {
			int remainingSize = size - allUsers.size();

			List<Long> finalExcludeIds = new ArrayList<>(excludeUserIds != null ? excludeUserIds : List.of());
			allUsers.forEach(user -> finalExcludeIds.add(user.getId()));

			List<User> additionalUsers = userRepository.fetchRandomUsersByPositionExcluding(
				userId, filterPosition, remainingSize, finalExcludeIds);
			allUsers.addAll(additionalUsers);
		}

		// 4단계: 시간 기반 셔플
		long timeSeed = System.currentTimeMillis() / (10 * 1000);
		Random random = new Random(timeSeed);
		Collections.shuffle(allUsers, random);

		return allUsers.stream().limit(size).collect(Collectors.toList());
	}

	/**
	 * 유사도 계산용 내부 레코드
	 */
	private record UserSimilarity(User user, double similarity) {}

	/**
	 * 신규 사용자 클러스터 즉시 배정
	 *
	 * @param newUser 신규 사용자
	 * @return 배정된 클러스터 ID
	 */
	public Integer assignNewUserToCluster(User newUser) {
		try {
			String clusterKey = newUser.getMbti().name() + "_" + newUser.getPosition().name();
			Integer clusterId = VECTOR_BASED_CLUSTER_MAP.get(clusterKey);

			if (clusterId == null) {
				log.warn("알 수 없는 조합: {}. 기본 클러스터 0 배정", clusterKey);
				return 0;
			}

			return clusterId;

		} catch (Exception e) {
			log.error("신규 사용자 클러스터 배정 중 오류", e);
			return 0;
		}
	}

	/**
	 * 벡터 기반 클러스터 맵 생성
	 *
	 * 동작 과정:
	 * 1. 모든 MBTI × Position 조합(96가지)의 10차원 벡터 계산
	 * 2. 사전 정의된 클러스터 중심점과 코사인 유사도 계산
	 * 3. 가장 유사한 클러스터에 배정
	 *
	 * @return MBTI_Position → 클러스터 ID 맵
	 */
	private static Map<String, Integer> createVectorBasedClusterMap() {
		Map<String, Integer> clusterMap = new HashMap<>();
		UserVectorizer vectorizer = new UserVectorizer();

		// 1단계: 모든 조합의 10차원 벡터 계산
		Map<String, double[]> vectorMap = new HashMap<>();
		for (Mbti mbti : Mbti.values()) {
			for (Position position : Position.values()) {
				String key = mbti.name() + "_" + position.name();

				double[] vector = new double[10]; // 10차원

				// MBTI 4차원 추가
				double[] mbtiBinary = vectorizer.getMbtiBinary(mbti);
				System.arraycopy(mbtiBinary, 0, vector, 0, 4);

				// Position 6차원 추가
				double[] positionVector = vectorizer.getPositionVector(position);
				System.arraycopy(positionVector, 0, vector, 4, 6);

				vectorMap.put(key, vector);
			}
		}

		// 2단계: 벡터 유사도 기반 클러스터 배정
		assignClustersBasedOnVectorSimilarity(vectorMap, clusterMap);

		log.info("벡터 기반 클러스터 맵 생성 완료: {}개 조합 분류", clusterMap.size());
		return clusterMap;
	}

	/**
	 * 벡터와 클러스터 중심점 간 유사도 계산하여 클러스터 배정
	 *
	 * 각 MBTI+Position 조합 벡터를 6개 클러스터 중심점과 비교하여
	 * 코사인 유사도가 가장 높은 클러스터에 배정
	 *
	 * @param vectorMap MBTI_Position → 9차원 벡터 맵
	 * @param clusterMap 결과를 저장할 클러스터 맵
	 */
	private static void assignClustersBasedOnVectorSimilarity(
		Map<String, double[]> vectorMap, Map<String, Integer> clusterMap) {

		// 클러스터 중심점 정의
		Map<Integer, double[]> clusterCentroids = createClusterCentroids();

		// 각 조합을 가장 유사한 클러스터에 배정
		for (Map.Entry<String, double[]> entry : vectorMap.entrySet()) {
			String key = entry.getKey();
			double[] vector = entry.getValue();

			int bestCluster = 0;
			double maxSimilarity = -1;

			// 모든 클러스터 중심점과 유사도 비교
			for (Map.Entry<Integer, double[]> centroid : clusterCentroids.entrySet()) {
				double similarity = calculateCosineSimilarity(vector, centroid.getValue());
				if (similarity > maxSimilarity) {
					maxSimilarity = similarity;
					bestCluster = centroid.getKey();
				}
			}

			clusterMap.put(key, bestCluster);
		}
	}

	/**
	 * 클러스터 중심점 정의
	 *
	 * 10차원 벡터 구조: [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
	 *
	 * 각 클러스터는 특정 성향의 개발자 그룹을 대표:
	 * - 클러스터 0: 분석적 백엔드 개발자 (NT + BACKEND/FULLSTACK/AI)
	 * - 클러스터 1: 창의적 프론트엔드 개발자 (NF + FRONTEND/UX)
	 * - 클러스터 2: 실용적 개발자 (ST + BACKEND/FRONTEND)
	 * - 클러스터 3: 소통형 개발자 (SF + 협업 중심)
	 * - 클러스터 4: PM 특화 (모든 MBTI + PM)
	 * - 클러스터 5: 풀스택/AI 특화 (E + FULLSTACK/AI)
	 *
	 * @return 클러스터 ID → 중심점 벡터 맵
	 */
	private static Map<Integer, double[]> createClusterCentroids() {
		Map<Integer, double[]> centroids = new HashMap<>();

		/**
		 * 클러스터 0: NT 계열 + 백엔드/풀스택/AI (분석적 개발자)
		 * [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
		 * [0,   0,   1,   0.5,  0.9,     0.3,      0.1,   0.2, 0.8,       0.9]
		 */
		centroids.put(0, new double[]{0, 0, 1, 0.5, 0.9, 0.3, 0.1, 0.2, 0.8, 0.9});

		/**
		 * 클러스터 1: NF 계열 + 프론트엔드/UX (창의적 개발자)
		 * [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
		 * [0.5, 0,   0,   0,   0.3,     0.8,      0.8,   0.3, 0.6,       0.4]
		 */
		centroids.put(1, new double[]{0.5, 0, 0, 0, 0.3, 0.8, 0.8, 0.3, 0.6, 0.4});

		/**
		 * 클러스터 2: ST 계열 + 백엔드/프론트 (실용적 개발자)
		 * [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
		 * [0.5, 1,   1,   1,   0.7,     0.7,      0.2,   0.2, 0.7,       0.6]
		 */
		centroids.put(2, new double[]{0.5, 1, 1, 1, 0.7, 0.7, 0.2, 0.2, 0.7, 0.6});

		/**
		 * 클러스터 3: SF 계열 + 협업 중심 (소통형 개발자)
		 * [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
		 * [1,   1,   0,   1,   0.3,     0.6,      0.6,   0.4, 0.5,       0.3]
		 */
		centroids.put(3, new double[]{1, 1, 0, 1, 0.3, 0.6, 0.6, 0.4, 0.5, 0.3});

		/**
		 * 클러스터 4: PM 특화 (모든 MBTI + PM)
		 * [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
		 * [0.5, 0.5, 0.5, 1,   0.2,     0.3,      0.4,   1.0, 0.3,       0.4]
		 */
		centroids.put(4, new double[]{0.5, 0.5, 0.5, 1, 0.2, 0.3, 0.4, 1.0, 0.3, 0.4});

		/**
		 * 클러스터 5: 풀스택/AI 특화 (E 계열 + FULLSTACK/AI)
		 * [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
		 * [1,   0.5, 0.5, 0.5, 0.8,     0.7,      0.4,   0.3, 1.0,       0.9]
		 */
		centroids.put(5, new double[]{1, 0.5, 0.5, 0.5, 0.8, 0.7, 0.4, 0.3, 1.0, 0.9});

		return centroids;
	}

	/**
	 * 두 벡터 간 코사인 유사도 계산
	 *
	 * 코사인 유사도 = (벡터1 · 벡터2) / (||벡터1|| × ||벡터2||)
	 * 결과: -1.0 ~ 1.0 (1.0에 가까울수록 유사)
	 *
	 * @param vector1 첫 번째 벡터
	 * @param vector2 두 번째 벡터
	 * @return 코사인 유사도 (-1.0 ~ 1.0)
	 */
	private static double calculateCosineSimilarity(double[] vector1, double[] vector2) {
		double dotProduct = 0.0;
		double norm1 = 0.0;
		double norm2 = 0.0;

		for (int i = 0; i < vector1.length; i++) {
			dotProduct += vector1[i] * vector2[i];
			norm1 += vector1[i] * vector1[i];
			norm2 += vector2[i] * vector2[i];
		}

		if (norm1 == 0.0 || norm2 == 0.0) {
			return 0.0;
		}

		return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
	}
}
