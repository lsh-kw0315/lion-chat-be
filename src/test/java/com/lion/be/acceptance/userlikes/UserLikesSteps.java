package com.lion.be.acceptance.userlikes;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class UserLikesSteps {

	// === 좋아요 관련 API 호출 메서드들 ===

	public static ExtractableResponse<Response> 좋아요를_누른다(
		RequestSpecification spec, String accessToken, Long targetUserId) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.log().all()
			.when()
			.post("/api/users/likes/{targetUserId}", targetUserId)
			.then()
			.log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 좋아요한_사용자_목록을_조회한다(
		RequestSpecification spec, String accessToken) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.auth().oauth2(accessToken)
			.log().all()
			.when()
			.get("/api/users/likes/lists")
			.then()
			.log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 인증없이_좋아요를_누른다(
		RequestSpecification spec, Long targetUserId) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.log().all()
			.when()
			.post("/api/users/likes/" + targetUserId)
			.then()
			.log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 인증없이_좋아요_목록을_조회한다(
		RequestSpecification spec) {
		return RestAssured
			.given()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.spec(spec)
			.log().all()
			.when()
			.get("/api/users/likes/lists")
			.then()
			.log().all()
			.extract();
	}

	// === 좋아요 검증 메서드들 ===

	public static void 좋아요_성공을_검증한다(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
	}

	public static void 좋아요_상태가_true임을_검증한다(ExtractableResponse<Response> response) {
		assertThat(response.jsonPath().getBoolean("isLiked")).isTrue();
	}

	public static void 좋아요_상태가_false임을_검증한다(ExtractableResponse<Response> response) {
		assertThat(response.jsonPath().getBoolean("isLiked")).isFalse();
	}

	public static void 좋아요_목록_조회_성공을_검증한다(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.jsonPath().getList("content")).isNotNull();
	}

	public static void 좋아요한_사용자가_포함됨을_검증한다(ExtractableResponse<Response> response, List<Long> expectedUserIds) {
		List<Long> actualUserIds = response.jsonPath().getList("content").stream()
			.map(card -> ((Number) ((Map<String, Object>) card).get("userId")).longValue())
			.toList();

		for (Long expectedId : expectedUserIds) {
			assertThat(actualUserIds).contains(expectedId);
		}

		System.out.println("예상 좋아요 사용자 ID: " + expectedUserIds);
		System.out.println("실제 좋아요 사용자 ID: " + actualUserIds);
	}

	public static void 좋아요_목록의_카드_정보를_검증한다(ExtractableResponse<Response> response) {
		List<Map<String, Object>> likedUsers = response.jsonPath().getList("content");

		if (!likedUsers.isEmpty()) {
			Map<String, Object> firstUser = likedUsers.get(0);

			assertThat(firstUser.get("userId")).isNotNull();
			assertThat(firstUser.get("nickname")).isNotNull();
			assertThat(firstUser.get("university")).isNotNull();
			assertThat(firstUser.get("position")).isNotNull();
			assertThat(firstUser.get("imageUrls")).isNotNull();
			assertThat(firstUser.get("bio")).isNotNull();
			assertThat((Boolean) firstUser.get("isLikedByMe")).isTrue(); // 좋아요 목록에서는 항상 true

			System.out.println("좋아요 목록 첫 번째 사용자 정보: " + firstUser);
		}
	}

	public static void 사용자_카드_정보_완성도를_검증한다(ExtractableResponse<Response> response) {
		List<Map<String, Object>> cards = response.jsonPath().getList("content");

		for (Map<String, Object> card : cards) {
			assertThat(card.get("userId")).isNotNull();
			assertThat((String) card.get("nickname")).isNotEmpty();
			assertThat(card.get("university")).isNotNull();
			assertThat(card.get("isUniversityVisible")).isNotNull();
			assertThat((String) card.get("position")).isNotEmpty();
			assertThat((List<?>) card.get("imageUrls")).isNotEmpty();
			assertThat((String) card.get("bio")).isNotEmpty();
			assertThat((Boolean) card.get("isLikedByMe")).isTrue();
		}

		System.out.println("검증된 카드 개수: " + cards.size());
	}

	// === 로그인 관련은 UserCardSteps에서 처리 ===
	// 중복 제거를 위해 로그인 메서드들은 UserCardSteps에서만 관리
}
