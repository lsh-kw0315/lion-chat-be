package com.lion.be.user.domain.entity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.user.domain.Mbti;
import com.lion.be.user.domain.Position;

/**
 * 사용자 벡터화 및 MBTI 호환성 계산 컴포넌트
 *
 * 주요 기능:
 * 1. User 객체를 10차원 수치 벡터로 변환 (클러스터링 및 유사도 계산용)
 * 2. MBTI 기반 사용자 간 호환성 점수 계산
 * 3. Position 간 유사도 벡터 제공
 *
 * 벡터 구조:
 * - MBTI 4차원: [E/I, S/N, T/F, J/P] (이진 값: 0 또는 1)
 * - Position 6차원: [BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI] (유사도 점수: 0.0~1.0)
 *
 * 사용처:
 * - UserCardFilterUtil의 클러스터링 로직
 * - 사용자 추천 시 유사도 계산
 */
@Component
public class UserVectorizer {

	/**
	 * MBTI 호환성 맵 (애플리케이션 시작 시 한 번만 초기화)
	 * 실제 MBTI 이론을 기반으로 한 찰떡궁합/충돌잦은궁합 데이터
	 */
	private static final Map<Mbti, Map<String, List<Mbti>>> COMPATIBILITY_MAP = createCompatibilityMap();

	/**
	 * User 객체를 10차원 벡터로 변환
	 *
	 * 벡터 구조: [E/I, S/N, T/F, J/P, BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
	 *
	 * 사용 목적:
	 * - Weka 라이브러리 기반 클러스터링
	 * - 사용자 간 코사인 유사도 계산
	 * - 추천 알고리즘의 입력 데이터
	 *
	 * @param user 벡터화할 사용자 객체
	 * @return 10차원 double 배열 (MBTI 4차원 + Position 6차원)
	 * @throws CustomException 온보딩 미완료 사용자인 경우
	 */
	@Deprecated //향후 쓸수도 있어서 내비둠 createVectorBasedClusterMap 내에서 이미 동일한 작업 진행
	public double[] vectorize(User user) {
		validateUser(user);

		double[] vector = new double[10]; // 10차원으로 확장

		// MBTI 4차원 벡터 생성 및 추가
		double[] mbtiBinary = getMbtiBinary(user.getMbti());
		System.arraycopy(mbtiBinary, 0, vector, 0, 4);

		// Position 6차원 벡터 생성 및 추가
		double[] positionVector = getPositionVector(user.getPosition());
		System.arraycopy(positionVector, 0, vector, 4, 6);

		return vector;
	}

	/**
	 * MBTI를 4차원 이진 벡터로 변환
	 *
	 * 각 차원은 MBTI의 4가지 선호도 지표를 나타냄:
	 * - 차원 0: E(외향)/I(내향) → E면 1, I면 0
	 * - 차원 1: S(감각)/N(직관) → S면 1, N면 0
	 * - 차원 2: T(사고)/F(감정) → T면 1, F면 0
	 * - 차원 3: J(판단)/P(인식) → J면 1, P면 0
	 *
	 * 예시:
	 * - INTJ → [0, 0, 1, 1]
	 * - ENFP → [1, 0, 0, 0]
	 *
	 * @param mbti MBTI 열거형
	 * @return 4차원 이진 벡터 [E/I, S/N, T/F, J/P]
	 */
	public double[] getMbtiBinary(Mbti mbti) {
		String mbtiStr = mbti.name();
		return new double[]{
			mbtiStr.charAt(0) == 'E' ? 1 : 0,  // E/I
			mbtiStr.charAt(1) == 'S' ? 1 : 0,  // S/N
			mbtiStr.charAt(2) == 'T' ? 1 : 0,  // T/F
			mbtiStr.charAt(3) == 'J' ? 1 : 0   // J/P
		};
	}

	/**
	 * Position을 6차원 유사도 벡터로 변환 (AI 포지션 추가)
	 *
	 * 각 Position이 다른 Position들과 얼마나 유사한지를 수치화
	 * 벡터 구조: [BACKEND, FRONTEND, UX_UI, PM, FULLSTACK, AI]
	 *
	 * 유사도 기준:
	 * - 자기 자신: 1.0 (완전 일치)
	 * - 기술적 유사성: 0.3~0.8 (개발 분야 공통점)
	 * - 협업 관계: 0.1~0.6 (업무 협업 빈도)
	 * - AI 관련: 0.7~0.9 (AI는 모든 개발 분야와 높은 연관성)
	 *
	 * AI 포지션 특징:
	 * - BACKEND와 높은 유사도 (데이터 처리, 서버 개발)
	 * - FRONTEND와 중간 유사도 (AI 모델 UI 통합)
	 * - FULLSTACK과 매우 높은 유사도 (전체적인 기술 스택 활용)
	 *
	 * @param position Position 열거형
	 * @return 6차원 유사도 벡터
	 */
	public double[] getPositionVector(Position position) {
		double[] vector = new double[6]; // 5차원 → 6차원으로 확장

		switch (position) {
			case BACKEND -> {
				vector[0] = 1.0;   // BACKEND (자기 자신)
				vector[1] = 0.3;   // FRONTEND (일부 공통 기술: API, 데이터베이스)
				vector[2] = 0.1;   // UX_UI (협업 정도)
				vector[3] = 0.2;   // PM (협업 정도)
				vector[4] = 0.8;   // FULLSTACK (높은 유사도)
				vector[5] = 0.8;   // AI (데이터 처리, ML 모델 서빙, API 개발)
			}
			case FRONTEND -> {
				vector[0] = 0.3;   // BACKEND
				vector[1] = 1.0;   // FRONTEND (자기 자신)
				vector[2] = 0.6;   // UX_UI (UI 관련 협업)
				vector[3] = 0.3;   // PM (기획 협업)
				vector[4] = 0.7;   // FULLSTACK
				vector[5] = 0.5;   // AI (AI 모델 UI 통합, 데이터 시각화)
			}
			case UX_UI -> {
				vector[0] = 0.1;   // BACKEND
				vector[1] = 0.6;   // FRONTEND
				vector[2] = 1.0;   // UX_UI (자기 자신)
				vector[3] = 0.4;   // PM (기획 협업)
				vector[4] = 0.4;   // FULLSTACK
				vector[5] = 0.3;   // AI (AI 서비스 UX 설계, 사용자 경험)
			}
			case PM -> {
				vector[0] = 0.2;   // BACKEND
				vector[1] = 0.3;   // FRONTEND
				vector[2] = 0.4;   // UX_UI
				vector[3] = 1.0;   // PM (자기 자신)
				vector[4] = 0.3;   // FULLSTACK
				vector[5] = 0.4;   // AI (AI 프로젝트 기획, 요구사항 정의)
			}
			case FULLSTACK -> {
				vector[0] = 0.8;   // BACKEND
				vector[1] = 0.7;   // FRONTEND
				vector[2] = 0.4;   // UX_UI
				vector[3] = 0.3;   // PM
				vector[4] = 1.0;   // FULLSTACK (자기 자신)
				vector[5] = 0.9;   // AI (전체 스택 + AI 통합 개발)
			}
			case AI -> {
				vector[0] = 0.8;   // BACKEND (ML 모델 서빙, 데이터 파이프라인)
				vector[1] = 0.5;   // FRONTEND (AI 서비스 UI, 실시간 추론 결과 표시)
				vector[2] = 0.3;   // UX_UI (AI 제품의 사용자 경험 고려)
				vector[3] = 0.4;   // PM (AI 제품 기획 이해 필요)
				vector[4] = 0.9;   // FULLSTACK (AI + 웹 서비스 통합)
				vector[5] = 1.0;   // AI (자기 자신)
			}
		}

		return vector;
	}

	/**
	 * 두 사용자 간 MBTI 호환성 점수 계산
	 *
	 * 실제 MBTI 이론을 바탕으로 한 호환성 데이터 활용
	 *
	 * 점수 체계:
	 * - 1.0: 동일한 MBTI (완벽한 이해)
	 * - 0.9: 찰떡궁합 (상호 보완적 관계)
	 * - 0.5: 무난한 관계 (보통 수준 호환성)
	 * - 0.1: 충돌 잦은 관계 (성향 차이로 인한 갈등 가능)
	 *
	 * 사용처:
	 * - 동일 클러스터 내 사용자 추천 시 우선순위 결정
	 * - PreferenceType.MBTI_FOCUSED 사용자의 추천 가중치
	 *
	 * @param user1 첫 번째 사용자
	 * @param user2 두 번째 사용자
	 * @return 0.1~1.0 범위의 호환성 점수
	 */
	public double calculateMbtiCompatibility(User user1, User user2) {
		Mbti mbti1 = user1.getMbti();
		Mbti mbti2 = user2.getMbti();

		// 같은 MBTI면 최고 호환성
		if (mbti1 == mbti2) {
			return 1.0;
		}

		Map<String, List<Mbti>> compatibilityInfo = COMPATIBILITY_MAP.get(mbti1);
		if (compatibilityInfo == null) {
			return 0.5; // 기본값 (정보 없는 경우)
		}

		// 찰떡궁합 체크 (상호 보완적 관계)
		if (compatibilityInfo.get("찰떡궁합").contains(mbti2)) {
			return 0.9;
		}

		// 충돌 잦은 궁합 체크 (갈등 가능성 높음)
		if (compatibilityInfo.get("충돌잦은궁합").contains(mbti2)) {
			return 0.1;
		}

		// 그 외는 무난한 관계
		return 0.5;
	}

	/**
	 * MBTI 호환성 맵 생성 (실제 MBTI 이론 기반 데이터)
	 *
	 * 각 MBTI 타입별로 다른 타입들과의 관계를 정의:
	 * - "찰떡궁합": 서로 다른 강점을 보완하며 시너지를 내는 관계
	 * - "충돌잦은궁합": 가치관이나 의사소통 방식의 차이로 갈등이 잦은 관계
	 *
	 * 일반적인 패턴:
	 * - NT(분석가) ↔ NF(외교관): 직관(N) 공통점으로 이해 가능, T/F 차이로 보완
	 * - ST(관리자) ↔ SF(탐험가): 현실감각(S) 공통점, T/F 차이로 균형
	 * - 같은 기질 내에서도 E/I, J/P 차이에 따른 보완 관계 존재
	 *
	 * @return MBTI → 호환성 정보 맵
	 */
	private static Map<Mbti, Map<String, List<Mbti>>> createCompatibilityMap() {
		Map<Mbti, Map<String, List<Mbti>>> map = new EnumMap<>(Mbti.class);

		// ISTJ - 현실주의적 관리자
		map.put(Mbti.ISTJ, Map.of(
			"찰떡궁합", List.of(Mbti.ESFP, Mbti.ESTP),
			"충돌잦은궁합", List.of(Mbti.ENFP, Mbti.INFP)
		));

		// ISFJ - 용감한 수호자
		map.put(Mbti.ISFJ, Map.of(
			"찰떡궁합", List.of(Mbti.ESTP, Mbti.ESFP),
			"충돌잦은궁합", List.of(Mbti.ENTP, Mbti.INFP)
		));

		// INFJ - 선의의 옹호자
		map.put(Mbti.INFJ, Map.of(
			"찰떡궁합", List.of(Mbti.ENFP, Mbti.ENTP),
			"충돌잦은궁합", List.of(Mbti.ESTP, Mbti.ISTP)
		));

		// INTJ - 용의주도한 전략가
		map.put(Mbti.INTJ, Map.of(
			"찰떡궁합", List.of(Mbti.ENFP, Mbti.ENTP),
			"충돌잦은궁합", List.of(Mbti.ESFP, Mbti.ISFP)
		));

		// ISTP - 만능 재주꾼
		map.put(Mbti.ISTP, Map.of(
			"찰떡궁합", List.of(Mbti.ISFJ, Mbti.ESFJ),
			"충돌잦은궁합", List.of(Mbti.ENFP, Mbti.ENFJ)
		));

		// ISFP - 호기심 많은 예술가
		map.put(Mbti.ISFP, Map.of(
			"찰떡궁합", List.of(Mbti.ISFJ, Mbti.ESFJ),
			"충돌잦은궁합", List.of(Mbti.ENTJ, Mbti.ENTP)
		));

		// INFP - 중재자
		map.put(Mbti.INFP, Map.of(
			"찰떡궁합", List.of(Mbti.ENFJ, Mbti.INFJ),
			"충돌잦은궁합", List.of(Mbti.ESTJ, Mbti.ISTJ)
		));

		// INTP - 논리술사
		map.put(Mbti.INTP, Map.of(
			"찰떡궁합", List.of(Mbti.ENFJ, Mbti.ENTJ),
			"충돌잦은궁합", List.of(Mbti.ISFJ, Mbti.ESFJ)
		));

		// ESTP - 사업가
		map.put(Mbti.ESTP, Map.of(
			"찰떡궁합", List.of(Mbti.ISFJ, Mbti.ISTJ),
			"충돌잦은궁합", List.of(Mbti.INFJ, Mbti.INFP)
		));

		// ESFP - 연예인
		map.put(Mbti.ESFP, Map.of(
			"찰떡궁합", List.of(Mbti.ISFJ, Mbti.ISTJ),
			"충돌잦은궁합", List.of(Mbti.INTJ, Mbti.INFJ)
		));

		// ENFP - 활동가
		map.put(Mbti.ENFP, Map.of(
			"찰떡궁합", List.of(Mbti.INFJ, Mbti.INTJ),
			"충돌잦은궁합", List.of(Mbti.ISTJ, Mbti.ESTJ)
		));

		// ENTP - 변론가
		map.put(Mbti.ENTP, Map.of(
			"찰떡궁합", List.of(Mbti.INFJ, Mbti.INFP),
			"충돌잦은궁합", List.of(Mbti.ISTJ, Mbti.ISFJ)
		));

		// ESTJ - 경영자
		map.put(Mbti.ESTJ, Map.of(
			"찰떡궁합", List.of(Mbti.ISFP, Mbti.ISTP),
			"충돌잦은궁합", List.of(Mbti.INFP, Mbti.ENFP)
		));

		// ESFJ - 집정관
		map.put(Mbti.ESFJ, Map.of(
			"찰떡궁합", List.of(Mbti.ISFP, Mbti.INFP),
			"충돌잦은궁합", List.of(Mbti.INTJ, Mbti.INTP)
		));

		// ENFJ - 주인공
		map.put(Mbti.ENFJ, Map.of(
			"찰떡궁합", List.of(Mbti.INFP, Mbti.INFJ),
			"충돌잦은궁합", List.of(Mbti.ISTP, Mbti.ESTP)
		));

		// ENTJ - 통솔자
		map.put(Mbti.ENTJ, Map.of(
			"찰떡궁합", List.of(Mbti.INTP, Mbti.INTJ),
			"충돌잦은궁합", List.of(Mbti.ISFP, Mbti.ESFP)
		));

		return map;
	}

	/**
	 * 사용자 유효성 검증
	 *
	 * 벡터화 및 호환성 계산에 필요한 데이터(MBTI, Position)가
	 * 모두 설정되었는지 확인
	 *
	 * @param user 검증할 사용자
	 * @throws CustomException 온보딩이 완료되지 않은 경우
	 */
	private void validateUser(User user) {
		if (!user.isOnboardingCompleted()) {
			throw new CustomException(ErrorCode.USER_ONBOARDING_NOT_COMPLETED);
		}
	}
}
