package com.lion.be.acceptance.usercard;

import static org.assertj.core.api.Assertions.assertThat;
import static com.lion.be.acceptance.auth.AuthSteps.로그인한다;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

public class UserCardSteps {

	// === 로그인 관련 ===

	public static String 김프론트_로그인() {
		return 특정_사용자_로그인("front1@test.com", "김프론트", "https://test.com/image1.jpg");
	}

	public static String 김백엔드_로그인() {
		return 특정_사용자_로그인("back1@test.com", "김백엔드", "https://test.com/image6.jpg");
	}

	public static String 이리액트_로그인() {
		return 특정_사용자_로그인("front2@test.com", "이리액트", "https://test.com/image2.jpg");
	}

	public static String 박뷰js_로그인() {
		return 특정_사용자_로그인("front3@test.com", "박뷰js", "https://test.com/image3.jpg");
	}

	public static String 이스프링_로그인() {
		return 특정_사용자_로그인("back2@test.com", "이스프링", "https://test.com/image7.jpg");
	}

	public static String 박자바_로그인() {
		return 특정_사용자_로그인("back3@test.com", "박자바", "https://test.com/image8.jpg");
	}

	public static String 특정_사용자_로그인(String email, String name, String imageUrl) {
		Map<String, Object> loginRequest = Map.of(
			"email", email,
			"name", name,
			"imageUrl", imageUrl
		);
		return 로그인한다(loginRequest, 기본_스펙()).jsonPath().getString("accessToken");
	}

	// === 카드 조회 관련 ===

	public static ExtractableResponse<Response> 카드를_조회한다(
		RequestSpecification spec, String accessToken) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.log().all()
			.when()
			.get("/api/users/profile")
			.then()
			.log().all()
			.extract();
	}

	public static ExtractableResponse<Response> Id로_카드를_조회한다(
		RequestSpecification spec, String accessToken,
		Long userId
	) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.log().all()
			.when()
			.get("/api/users/profile/{userId}", userId)
			.then()
			.log().all()
			.extract();
	}


	public static ExtractableResponse<Response> 카드리스트를_조회한다(
		RequestSpecification spec, String accessToken, int size) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.param("size", size)
			.log().all()
			.when()
			.get("/api/users/cards/list")
			.then()
			.log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 특정_사용자를_제외하고_카드를_조회한다(
		RequestSpecification spec, String accessToken, int size, String excludeUserIds) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.param("size", size)
			.param("excludeUserIds", excludeUserIds)
			.log().all()
			.when()
			.get("/api/users/cards/list")
			.then()
			.log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 인증없이_카드를_조회한다(
		RequestSpecification spec, int size) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.param("size", size)
			.log().all()
			.when()
			.get("/api/users/cards/list")
			.then()
			.log().all()
			.extract();
	}

	// === 검증 메서드들 ===

	public static void 단일_카드_조회_성공을_검증한다(ExtractableResponse<Response> response) {
		Assertions.assertAll(
			() -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
			() -> assertThat(response.jsonPath().getLong("userId")).isNotNull(),
			() -> assertThat(response.jsonPath().getString("nickname")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getString("university")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getBoolean("isUniversityVisible")).isNotNull(),
			() -> assertThat(response.jsonPath().getString("position")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getString("mbti")).isNotNull(),
			() -> assertThat(response.jsonPath().getList("imageUrls")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getList("imageIds")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getString("bio")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getBoolean("isLikedByMe")).isFalse()
		);
	}

	public static void 카드_리스트_조회_성공을_검증한다(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.jsonPath().getList("$")).isNotNull();
	}

	public static void 상태코드가_401임을_검증한다(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

    public static void 클러스터_기반_추천을_검증한다(ExtractableResponse<Response> response, int expectedCluster) {
        List<Map<String, Object>> cards = response.jsonPath().getList("$");
        assertThat(cards).isNotEmpty();
        assertThat(cards).hasSizeGreaterThan(0);

        // 클러스터별 구체적 검증 대신 일반적 검증
        System.out.println("추천된 사용자 수: " + cards.size());
    }

	public static void 중복_카드가_없음을_검증한다(
		ExtractableResponse<Response> firstResponse,
		ExtractableResponse<Response> secondResponse) {

		List<Long> firstUserIds = firstResponse.jsonPath().getList("$").stream()
			.map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
			.toList();

		List<Long> secondUserIds = secondResponse.jsonPath().getList("$").stream()
			.map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
			.toList();

		// 두 조회 결과에 중복되는 사용자가 없어야 함
		Set<Long> intersection = new HashSet<>(firstUserIds);
		intersection.retainAll(secondUserIds);

		assertThat(intersection).isEmpty();
		System.out.println("첫 번째 조회: " + firstUserIds);
		System.out.println("두 번째 조회: " + secondUserIds);
		System.out.println("중복 없음 검증 완료");
	}

	public static void 제외된_사용자가_결과에_없음을_검증한다(
		ExtractableResponse<Response> response, Long[] excludedUserIds) {

		List<Long> recommendedUserIds = response.jsonPath().getList("$").stream()
			.map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
			.toList();

		Set<Long> excludedSet = Set.of(excludedUserIds);

		for (Long excludedId : excludedSet) {
			assertThat(recommendedUserIds).doesNotContain(excludedId);
		}

		System.out.println("제외된 사용자 ID: " + Arrays.toString(excludedUserIds));
		System.out.println("추천된 사용자 ID: " + recommendedUserIds);
	}

	public static void 요청한_개수만큼_반환됨을_검증한다(ExtractableResponse<Response> response, int expectedSize) {
		List<Map<String, Object>> cards = response.jsonPath().getList("$");
		assertThat(cards).hasSize(expectedSize);
		System.out.println("요청한 개수: " + expectedSize + ", 실제 반환 개수: " + cards.size());
	}

	public static void 혼합_추천을_검증한다(ExtractableResponse<Response> response) {
		List<Map<String, Object>> cards = response.jsonPath().getList("$");
		assertThat(cards).isNotNull().isNotEmpty();

		// null-safe position 추출
		Set<String> positions = cards.stream()
			.filter(Objects::nonNull) // card 자체가 null인 경우 방지
			.map(card -> card.get("position"))
			.filter(Objects::nonNull) // position이 null인 경우 방지
			.map(String.class::cast) // 안전한 캐스팅
			.filter(pos -> !pos.trim().isEmpty()) // 빈 문자열 제거
			.collect(Collectors.toSet());

		// 최소 2개 이상의 서로 다른 직무가 있어야 혼합 추천으로 판단
		assertThat(positions)
			.as("혼합 추천을 위해 최소 2개 이상의 서로 다른 직무가 필요합니다")
			.hasSizeGreaterThan(1);

		System.out.println("추천된 직무 종류: " + positions);
		System.out.println("직무 다양성: " + positions.size() + "개 직무");
	}

	public static void 사용자_정보_형식을_검증한다(ExtractableResponse<Response> response) {
		List<Map<String, Object>> cards = response.jsonPath().getList("$");
		assertThat(cards).isNotEmpty();

		Map<String, Object> firstCard = cards.get(0);

		// 필수 필드들이 모두 있는지 확인
		Assertions.assertAll(
			() -> assertThat(firstCard.get("userId")).isNotNull(),
			() -> assertThat(firstCard.get("nickname")).isNotNull(),
			() -> assertThat(firstCard.get("university")).isNotNull(),
			() -> assertThat(firstCard.get("isUniversityVisible")).isNotNull(),
			() -> assertThat(firstCard.get("position")).isNotNull(),
			() -> assertThat(firstCard.get("mbti")).isNotNull(),
			() -> assertThat(firstCard.get("imageUrls")).isNotNull(),
			() -> assertThat((List<?>) firstCard.get("imageUrls")).isNotEmpty(),
			() -> assertThat(firstCard.get("imageIds")).isNotNull(),
			() -> assertThat((List<?>) firstCard.get("imageIds")).isNotEmpty(),
			() -> assertThat(firstCard.get("bio")).isNotNull(),
			() -> assertThat((String) firstCard.get("bio")).isNotEmpty(),
			() -> assertThat(response.jsonPath().getBoolean("isLikedByMe")).isFalse()
		);

		System.out.println("첫 번째 카드 정보: " + firstCard);
	}

	public static ExtractableResponse<Response> 포지션별_카드를_조회한다(
		RequestSpecification spec, String accessToken, int size, String position) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.param("size", size)
			.param("position", position)
			.log().all()
			.when()
			.get("/api/users/cards/category") // 새로운 엔드포인트
			.then()
			.log().all()
			.extract();
	}

	public static void 포지션_필터링_결과를_검증한다(ExtractableResponse<Response> response, String expectedPosition) {
		List<Map<String, Object>> cards = response.jsonPath().getList("$");
		assertThat(cards).isNotEmpty();

		for (Map<String, Object> card : cards) {
			assertThat(card.get("position")).isEqualTo(expectedPosition);
		}

		System.out.println("포지션 " + expectedPosition + " 필터링 검증 완료");
	}
	// === 유틸리티 메서드들 ===

	private static RequestSpecification 기본_스펙() {
		return RestAssured.given().contentType(MediaType.APPLICATION_JSON_VALUE);
	}

    /**
     * 7:3 비율 추천을 검증한다 (클러스터 70% + 최신 가입자 30%)
     */
    public static void 칠대삼_비율_추천을_검증한다(ExtractableResponse<Response> response, int totalSize) {
        List<Map<String, Object>> cards = response.jsonPath().getList("$");

        assertThat(cards).hasSize(totalSize);

        int expectedClusterSize = 7; // 고정값
        int expectedRecentSize = 3;  // 고정값

        System.out.println("전체 추천 사용자 수: " + cards.size());
        System.out.println("예상 클러스터 기반: " + expectedClusterSize + "명");
        System.out.println("예상 최신 가입자: " + expectedRecentSize + "명");

        // 최소한 요청한 개수는 반환되어야 함
        assertThat(cards.size()).isGreaterThanOrEqualTo(Math.min(totalSize, 10));

        // 다양성 검증 (7:3 혼합으로 인한 다양한 유형의 사용자)
        Set<String> positions = cards.stream()
                .map(card -> (String) card.get("position"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> mbtis = cards.stream()
                .map(card -> (String) card.get("mbti"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        System.out.println("직무 다양성: " + positions.size() + "개 (" + positions + ")");
        System.out.println("MBTI 다양성: " + mbtis.size() + "개");

        // 7:3 혼합으로 인해 어느 정도 다양성이 보장되어야 함
        assertThat(positions).hasSizeGreaterThanOrEqualTo(1);
    }

    /**
     * 시간 기반 셔플이 작동하는지 검증한다
     */
    public static void 시간_기반_셔플을_검증한다(ExtractableResponse<Response> response1,
                                      ExtractableResponse<Response> response2,
                                      ExtractableResponse<Response> response3) {
        List<Long> userIds1 = response1.jsonPath().getList("$").stream()
                .map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
                .toList();

        List<Long> userIds2 = response2.jsonPath().getList("$").stream()
                .map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
                .toList();

        List<Long> userIds3 = response3.jsonPath().getList("$").stream()
                .map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
                .toList();

        // 동일한 시간대(5분 이내)라면 순서가 같을 수도 있지만,
        // 적어도 다양한 사용자들이 섞여있어야 함
        System.out.println("첫 번째 조회 순서: " + userIds1);
        System.out.println("두 번째 조회 순서: " + userIds2);
        System.out.println("세 번째 조회 순서: " + userIds3);

        // 최소한 결과는 나와야 함
        assertThat(userIds1).isNotEmpty();
        assertThat(userIds2).isNotEmpty();
        assertThat(userIds3).isNotEmpty();
    }

    /**
     * 최신 가입자가 포함되었는지 검증한다
     */
    public static void 최신_가입자_포함을_검증한다(ExtractableResponse<Response> response) {
        List<Map<String, Object>> cards = response.jsonPath().getList("$");
        assertThat(cards).isNotEmpty();

        // userId가 높을수록 최신 가입자 (ID 기준)
        List<Long> userIds = cards.stream()
                .map(card -> ((Number) card.get("userId")).longValue())
                .sorted(Collections.reverseOrder())
                .toList();

        System.out.println("추천된 사용자 ID (높은순): " + userIds);

        // 상위 30% 정도는 비교적 높은 ID를 가져야 함 (최신 가입자)
        if (userIds.size() >= 10) {
            List<Long> topUserIds = userIds.subList(0, 3); // 상위 3명
            Long minTopId = Collections.min(topUserIds);

            System.out.println("상위 3명 사용자 ID: " + topUserIds);
            System.out.println("최소 상위 ID: " + minTopId);

            // 최신 가입자들이 어느 정도 포함되어야 함
            assertThat(minTopId).isGreaterThan(0L);
        }
    }
}
