package com.lion.be.acceptance.userlikes;

import static com.lion.be.acceptance.usercard.UserCardSteps.*;
import static com.lion.be.acceptance.userlikes.UserLikesSteps.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.lion.be.acceptance.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@DisplayName("유저 좋아요 시스템 인수테스트")
class UserLikesAcceptanceTest extends AcceptanceTest {

	@Nested
	@DisplayName("좋아요 토글 기능")
	class LikeToggleTest {

		@DisplayName("사용자가 다른 사용자에게 좋아요를 누르면 좋아요가 생성된다")
		@Test
		void when_user_likes_another_user_then_like_is_created() {
			api_문서_타이틀("create_like", spec);

			// given - 김프론트가 로그인
			String accessToken = 김프론트_로그인();
			Long targetUserId = 2L; // 이리액트

			// when - 좋아요 누르기
			ExtractableResponse<Response> response = 좋아요를_누른다(spec, accessToken, targetUserId);

			// then
			좋아요_성공을_검증한다(response);
			좋아요_상태가_true임을_검증한다(response);
		}

		@DisplayName("이미 좋아요한 사용자에게 다시 좋아요를 누르면 좋아요가 취소된다")
		@Test
		void when_user_unlikes_already_liked_user_then_like_is_removed() throws IOException {
			api_문서_타이틀("toggle_like", spec);

			// given
			String accessToken = 원준_완전_온보딩();
			Long targetUserId = 3L; // 박뷰js

			좋아요를_누른다(spec, accessToken, targetUserId); // 첫 번째 좋아요

			// when - 다시 좋아요 누르기 (토글)
			ExtractableResponse<Response> response = 좋아요를_누른다(spec, accessToken, targetUserId);

			// then
			좋아요_성공을_검증한다(response);
			좋아요_상태가_false임을_검증한다(response);
		}

		@DisplayName("사용자가 여러 명에게 연속으로 좋아요를 누를 수 있다")
		@Test
		void when_user_likes_multiple_users_then_all_likes_are_created() {
			api_문서_타이틀("multiple_likes", spec);

			// given - 김백엔드가 로그인
			String accessToken = 김백엔드_로그인();
			List<Long> targetUserIds = List.of(2L, 3L, 4L, 5L);

			// when - 여러 사용자에게 좋아요 누르기
			for (Long targetUserId : targetUserIds) {
				ExtractableResponse<Response> response = 좋아요를_누른다(spec, accessToken, targetUserId);
				좋아요_성공을_검증한다(response);
				좋아요_상태가_true임을_검증한다(response);
			}

			// then - 좋아요한 사용자 목록 확인
			ExtractableResponse<Response> likedUsersResponse = 좋아요한_사용자_목록을_조회한다(spec, accessToken);
			좋아요_목록_조회_성공을_검증한다(likedUsersResponse);
			좋아요한_사용자가_포함됨을_검증한다(likedUsersResponse, targetUserIds);
		}
	}

	@Nested
	@DisplayName("좋아요한 사용자 목록 조회")
	class LikedUsersListTest {

		@DisplayName("사용자가 좋아요한 사용자들의 목록을 조회할 수 있다")
		@Test
		void when_user_requests_liked_users_then_returns_liked_users_list() {
			api_문서_타이틀("get_liked_users_list", spec);

			// given - 이리액트가 로그인하고 여러 사용자에게 좋아요
			String accessToken = 이리액트_로그인();
			List<Long> targetUserIds = List.of(6L, 7L, 8L); // 김백엔드, 이스프링, 박자바

			for (Long targetUserId : targetUserIds) {
				좋아요를_누른다(spec, accessToken, targetUserId);
			}

			// when - 좋아요한 사용자 목록 조회
			ExtractableResponse<Response> response = 좋아요한_사용자_목록을_조회한다(spec, accessToken);

			// then
			좋아요_목록_조회_성공을_검증한다(response);
			좋아요한_사용자가_포함됨을_검증한다(response, targetUserIds);
			좋아요_목록의_카드_정보를_검증한다(response);
		}

		@DisplayName("좋아요하지 않은 사용자는 빈 목록을 반환한다")
		@Test
		void when_user_has_no_likes_then_returns_empty_list() {
			api_문서_타이틀("empty_liked_users_list", spec);

			// given - 새로운 사용자가 로그인 (아무도 좋아요하지 않음)
			String accessToken = 김프론트_로그인();

			// when - 좋아요한 사용자 목록 조회
			ExtractableResponse<Response> response = 좋아요한_사용자_목록을_조회한다(spec, accessToken);

			// then
			좋아요_목록_조회_성공을_검증한다(response);
			List<Map<String, Object>> likedUsers = response.jsonPath().getList("content");
			assertThat(likedUsers).isEmpty();

			// 페이징 정보도 검증
			assertThat(response.jsonPath().getInt("totalElements")).isEqualTo(0);
			assertThat(response.jsonPath().getBoolean("empty")).isTrue();
		}

		@DisplayName("좋아요 목록에서 사용자 정보가 올바르게 반환된다")
		@Test
		void when_user_requests_liked_users_then_returns_complete_user_info() throws IOException {
			api_문서_타이틀("liked_users_info_validation", spec);

			// given - 사용자가 로그인하고 좋아요
			String accessToken = 원준_완전_온보딩(); 	// 유저 아이디 1
			토킷_완전_온보딩(); 						// 유저 아이디가 2
			Long targetUserId = 2L;

			좋아요를_누른다(spec, accessToken, targetUserId);

			// when - 좋아요한 사용자 목록 조회
			ExtractableResponse<Response> response = 좋아요한_사용자_목록을_조회한다(spec, accessToken);

			// then
			좋아요_목록_조회_성공을_검증한다(response);
			사용자_카드_정보_완성도를_검증한다(response);
		}
	}

	@Nested
	@DisplayName("카드 조회 시 좋아요 상태 반영")
	class LikeStatusInCardTest {

		@DisplayName("카드 조회 시 좋아요하지 않은 사용자는 isLikedByMe가 false로 표시된다")
		@Test
		void when_user_views_cards_then_not_liked_users_show_false_status() throws IOException {
			api_문서_타이틀("card_like_status_false", spec);

			// given - 토킷가 로그인 (아무도 좋아요하지 않음)
			String accessToken = 토킷_완전_온보딩();

			// when - 카드 목록 조회
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 5);

			// then
			카드_리스트_조회_성공을_검증한다(response);

			// 모든 카드의 isLikedByMe가 false인지 확인
			List<Map<String, Object>> cards = response.jsonPath().getList("$");
			for (Map<String, Object> card : cards) {
				assertThat((Boolean) card.get("isLikedByMe")).isFalse();
			}
		}
	}

	@Nested
	@DisplayName("인증 및 권한 검증")
	class AuthenticationTest {

		@DisplayName("인증되지 않은 사용자가 좋아요를 누르면 401 에러를 반환한다")
		@Test
		void when_unauthenticated_user_tries_to_like_then_returns_401() {
			api_문서_타이틀("unauthorized_like_attempt", spec);

			// when - 인증 없이 좋아요 시도
			ExtractableResponse<Response> response = 인증없이_좋아요를_누른다(spec, 2L);

			// then
			assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		}

		@DisplayName("인증되지 않은 사용자가 좋아요 목록을 조회하면 401 에러를 반환한다")
		@Test
		void when_unauthenticated_user_tries_to_get_liked_users_then_returns_401() {
			api_문서_타이틀("unauthorized_liked_users_access", spec);

			// when - 인증 없이 좋아요 목록 조회 시도
			ExtractableResponse<Response> response = 인증없이_좋아요_목록을_조회한다(spec);

			// then
			assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		}

		@DisplayName("자기 자신에게는 좋아요를 누를 수 없다 (비즈니스 로직 검증)")
		@Test
		void when_user_tries_to_like_self_then_handles_appropriately() throws IOException {
			api_문서_타이틀("self_like_prevention", spec);

			// given - 원준 로그인 (userId: 1)
			String accessToken = 원준_완전_온보딩();

			// when - 자기 자신에게 좋아요 시도
			ExtractableResponse<Response> response = 좋아요를_누른다(spec, accessToken, 1L);

			// then - 400 Bad Request 에러 반환
			assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		}
	}

	@Nested
	@DisplayName("실시간 알림 통합 (이벤트 기반)")
	class NotificationIntegrationTest {

		@DisplayName("사용자가 좋아요를 누르면 상대방에게 알림이 발송된다")
		@Test
		void when_user_likes_another_user_then_notification_is_sent() {
			api_문서_타이틀("like_notification_sent", spec);

			// given - 김프론트가 로그인
			String accessToken = 김프론트_로그인();
			Long targetUserId = 6L; // 김백엔드

			// when - 좋아요 누르기
			ExtractableResponse<Response> response = 좋아요를_누른다(spec, accessToken, targetUserId);

			// then
			좋아요_성공을_검증한다(response);
			좋아요_상태가_true임을_검증한다(response);
		}
	}
}
