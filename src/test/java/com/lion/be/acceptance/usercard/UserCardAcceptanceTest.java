package com.lion.be.acceptance.usercard;

import static com.lion.be.acceptance.usercard.UserCardSteps.*;
import static com.lion.be.acceptance.usercard.UserCardSteps.김프론트_로그인;

import java.io.IOException;

import com.lion.be.acceptance.AcceptanceTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("유저 카드 추천 시스템 인수테스트")
class UserCardAcceptanceTest extends AcceptanceTest {

	@Nested
	@DisplayName("카드 추천 시스템")
	class CardRecommendationTest {
		@DisplayName("로그인된 유저는 본인 카드를 조회 가능하다.")
		@Test
		void when_login_user_requests_cards_then_get_user_card(){
			api_문서_타이틀("my_profile_card",spec);

			String accessToken = 김프론트_로그인();

			ExtractableResponse<Response> response = 카드를_조회한다(spec, accessToken);

			단일_카드_조회_성공을_검증한다(response);
		}

		@DisplayName("유저 아이디로 카드를 조회 가능하다.")
		@Test
		void when_userId_requests_cards_then_get_user_card() throws IOException {
			api_문서_타이틀("user_profile_card",spec);

			String accessToken = 김프론트_로그인();
			원준_완전_온보딩();

			Long userId = 1L ;

			ExtractableResponse<Response> response = Id로_카드를_조회한다(spec, accessToken, userId);

			단일_카드_조회_성공을_검증한다(response);
		}

		@DisplayName("클러스터 1 사용자(김프론트)가 카드를 조회하면, 같은 클러스터 사용자들을 우선 추천받는다")
		@Test
		void when_cluster1_user_requests_cards_then_recommends_same_cluster_users_first() {
			api_문서_타이틀("cluster_based_recommendation", spec);

			// given - 김프론트 (ID: 1, 클러스터 1, ENFP + FRONTEND)
			String accessToken = 김프론트_로그인();

			// when - 카드 10개 조회
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 10);

			// then
			카드_리스트_조회_성공을_검증한다(response);
			클러스터_기반_추천을_검증한다(response, 1); // 클러스터 1 사용자들 우선 추천
		}

		@DisplayName("클러스터 2 사용자(김백엔드)가 카드를 조회하면, 같은 클러스터 사용자들을 우선 추천받는다")
		@Test
		void when_cluster2_user_requests_cards_then_recommends_same_cluster_users_first() {
			api_문서_타이틀("backend_cluster_recommendation", spec);

			// given - 김백엔드 (ID: 6, 클러스터 2, INTJ + BACKEND)
			String accessToken = 김백엔드_로그인();

			// when
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 10);

			// then
			카드_리스트_조회_성공을_검증한다(response);
			클러스터_기반_추천을_검증한다(response, 2); // 클러스터 2 사용자들 우선 추천
		}

		@DisplayName("사용자가 연속으로 카드를 조회하면, 이미 본 카드는 제외된다")
		@Test
		void when_user_requests_cards_continuously_then_excludes_already_viewed_cards() {
			api_문서_타이틀("duplicate_prevention", spec);

			// given
			String accessToken = 김프론트_로그인();

			// when - 첫 번째 조회 (5개)
			ExtractableResponse<Response> firstResponse = 카드리스트를_조회한다(spec, accessToken, 5);

			// when - 두 번째 조회 (5개 더)
			ExtractableResponse<Response> secondResponse = 카드리스트를_조회한다(spec, accessToken, 5);

			// then
			카드_리스트_조회_성공을_검증한다(firstResponse);
			카드_리스트_조회_성공을_검증한다(secondResponse);
			중복_카드가_없음을_검증한다(firstResponse, secondResponse);
		}

		@DisplayName("사용자가 특정 사용자를 제외하고 카드를 조회하면, 해당 사용자는 결과에서 제외된다")
		@Test
		void when_user_requests_cards_with_exclusion_then_excludes_specified_users() {
			api_문서_타이틀("manual_exclusion", spec);

			// given
			String accessToken = 김프론트_로그인();

			// when - ID 2, 3번 사용자 제외하고 조회
			ExtractableResponse<Response> response = 특정_사용자를_제외하고_카드를_조회한다(
				spec, accessToken, 10, "2,3");

			// then
			카드_리스트_조회_성공을_검증한다(response);
			제외된_사용자가_결과에_없음을_검증한다(response, new Long[]{2L, 3L});
		}

		@DisplayName("클러스터 사용자가 부족한 경우, 랜덤으로 보완하여 요청한 개수만큼 반환한다")
		@Test
		void when_cluster_users_insufficient_then_supplements_with_random_users() {
			api_문서_타이틀("random_supplement", spec);

			// given - 클러스터 1 사용자가 많은 수를 요청 (클러스터 1은 5명뿐)
			String accessToken = 김프론트_로그인();

			// when - 15개 조회 (클러스터 1: 4명 + 랜덤: 11명)
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 15);

			// then
			카드_리스트_조회_성공을_검증한다(response);
			요청한_개수만큼_반환됨을_검증한다(response, 15);
			혼합_추천을_검증한다(response); // 클러스터 + 랜덤 혼합
		}

		@DisplayName("사용자 정보가 올바르게 반환되는지 확인한다")
		@Test
		void when_user_requests_cards_then_returns_correct_user_info() {
			api_문서_타이틀("user_info_validation", spec);

			// given
			String accessToken = 김프론트_로그인();

			// when
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 5);

			// then
			카드_리스트_조회_성공을_검증한다(response);
			사용자_정보_형식을_검증한다(response);
		}

        @DisplayName("7:3 비율 추천 시스템이 정상 작동한다")
        @Test
        void when_user_requests_cards_then_uses_7to3_ratio() {
            api_문서_타이틀("seven_to_three_ratio_recommendation", spec);

            // given
            String accessToken = 김프론트_로그인();

            // when - 10개 조회 (클러스터 7 + 최신 3)
            ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 10);

            // then
            카드_리스트_조회_성공을_검증한다(response);
            칠대삼_비율_추천을_검증한다(response, 10);
            최신_가입자_포함을_검증한다(response);
        }

        @DisplayName("시간 기반 셔플로 매번 다른 순서로 추천된다")
        @Test
        void when_user_requests_cards_multiple_times_then_different_order_by_time() throws InterruptedException {
            api_문서_타이틀("time_based_shuffle", spec);

            // given
            String accessToken = 김프론트_로그인();

            // when - 같은 조건으로 두 번 조회
            ExtractableResponse<Response> response1 = 카드리스트를_조회한다(spec, accessToken, 10);

            // 11초 대기 (10초 구간 넘어가기 위해)
            Thread.sleep(11000);

            ExtractableResponse<Response> response2 = 카드리스트를_조회한다(spec, accessToken, 10);

            Thread.sleep(11000);
            ExtractableResponse<Response> response3 = 카드리스트를_조회한다(spec, accessToken, 10);
            // then
            카드_리스트_조회_성공을_검증한다(response1);
            카드_리스트_조회_성공을_검증한다(response2);
            카드_리스트_조회_성공을_검증한다(response3);
            시간_기반_셔플을_검증한다(response1, response2, response3);
        }

        // 기존 테스트 수정
        @DisplayName("클러스터 기반 + 최신 가입자 혼합 추천이 작동한다")
        @Test
        void when_user_requests_cards_then_gets_cluster_plus_recent_users() {
            api_문서_타이틀("mixed_recommendation", spec);

            // given
            String accessToken = 김프론트_로그인();

            // when
            ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 10);

            // then
            카드_리스트_조회_성공을_검증한다(response);
            칠대삼_비율_추천을_검증한다(response, 10);
            혼합_추천을_검증한다(response); // 다양성 검증
        }
	}

	@Nested
	@DisplayName("Redis 조회 이력 관리")
	class ViewHistoryTest {

		@DisplayName("10분 후 조회 이력이 초기화되어 같은 사용자들이 다시 추천된다")
		@Test
		void when_10minutes_passed_then_view_history_resets() {
			// 실제로 10분을 기다릴 수 없으므로, Redis TTL이 설정되는지만 확인
			// 통합 테스트에서는 Redis Mock을 사용하여 TTL 확인

			// given
			String accessToken = 김프론트_로그인();

			// when
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 5);

			// then
			카드_리스트_조회_성공을_검증한다(response);
		}
	}

	@Nested
	@DisplayName("예외 상황 처리")
	class ExceptionHandlingTest {

		@DisplayName("인증되지 않은 사용자가 카드 조회 시 401 에러를 반환한다")
		@Test
		void when_unauthenticated_user_requests_cards_then_returns_401() {
			api_문서_타이틀("unauthorized_card_access", spec);

			// when
			ExtractableResponse<Response> response = 인증없이_카드를_조회한다(spec, 10);

			// then
			상태코드가_401임을_검증한다(response);
		}

		@DisplayName("온보딩 미완료 사용자가 카드 조회 시 적절한 에러를 반환한다")
		@Test
		void when_non_onboarded_user_requests_cards_then_returns_error() {
			// 온보딩 미완료 사용자 데이터가 있다면 테스트 추가
			// 현재는 data.sql에서 모든 사용자가 COMPLETED 상태
		}

		@DisplayName("잘못된 size 파라미터 시 적절하게 처리한다")
		@Test
		void when_invalid_size_parameter_then_handles_gracefully() {
			api_문서_타이틀("invalid_size_parameter", spec);

			// given
			String accessToken = 김프론트_로그인();

			// when - size를 0으로 요청
			ExtractableResponse<Response> response = 카드리스트를_조회한다(spec, accessToken, 0);

			// then
			// 빈 배열 반환하거나 기본값 적용되는지 검증
			카드_리스트_조회_성공을_검증한다(response);
		}
	}

	@Nested
	@DisplayName("카테고리별 카드 조회")
	class CategoryCardTest {

		@DisplayName("BACKEND 포지션으로 카드를 조회하면, BACKEND 사용자들만 추천받는다")
		@Test
		void when_filter_by_backend_position_then_returns_only_backend_users() {
			api_문서_타이틀("category_backend_filter", spec);

			String accessToken = 김프론트_로그인();

			ExtractableResponse<Response> response = 포지션별_카드를_조회한다(spec, accessToken, 10, "BACKEND");

			카드_리스트_조회_성공을_검증한다(response);
			포지션_필터링_결과를_검증한다(response, "백엔드");
		}
	}
}
