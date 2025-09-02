package com.lion.be.acceptance.chat;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ChatSteps {

    public static ExtractableResponse<Response> _1대1_채팅방을_생성_또는_조회한다(
            String accessToken, Long receiverId, RequestSpecification spec) {
        Map<String, Long> requestBody = Map.of("id", receiverId);
        return RestAssured
                .given().log().all()
                .spec(spec)
                .auth().oauth2(accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .when()
                .post("/api/chatrooms/init")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 자신의_채팅방_목록을_조회한다(
            String accessToken, RequestSpecification spec) {
        return RestAssured
                .given().log().all()
                .spec(spec)
                .auth().oauth2(accessToken)
                .when()
                .get("/api/chatrooms")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 채팅방_참가자_정보를_조회한다(
            String accessToken, Long chatRoomId, RequestSpecification spec
    ) {
        return RestAssured
                .given().log().all()
                .spec(spec)
                .auth().oauth2(accessToken)
                .queryParam("roomId", chatRoomId)
                .when()
                .get("/api/chatrooms/context")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 채팅방에_메시지를_전송한다(
            String accessToken, Long chatRoomId, String content, RequestSpecification spec) {
        Map<String, Object> requestBody = Map.of("chatRoomId", chatRoomId, "content", content);
        return RestAssured
                .given().log().all()
                .spec(spec)
                .auth().oauth2(accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .when()
                // 새로 만든 테스트 전용 API 경로로 수정
                .post("/api/test/chat/messages")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 채팅방의_초기_메시지_목록을_조회한다(
            String accessToken, Long chatRoomId, String lastId, RequestSpecification spec) {
        return RestAssured
                .given().log().all()
                .spec(spec)
                .auth().oauth2(accessToken)
                .queryParam("roomId", chatRoomId)
                .queryParam("lastId", lastId)
                .when()
                .get("/api/chatrooms/chats/messages")
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 채팅방의_메시지_목록을_조회한다(
            String accessToken, Long chatRoomId, String lastId, RequestSpecification spec) {
        return RestAssured
                .given().log().all()
                .spec(spec)
                .auth().oauth2(accessToken)
                .queryParam("roomId", chatRoomId)
                .queryParam("lastId", lastId)
                .when()
                .get("/api/chatrooms/chats/messages")
                .then().log().all()
                .extract();
    }

    public static void 채팅방_생성_응답을_검증한다(ExtractableResponse<Response> response) {
        Assertions.assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getLong("chatRoomId")).isNotNull().isPositive()
        );
    }

    public static void 기존_채팅방_조회_응답을_검증한다(ExtractableResponse<Response> response, Long expectedChatRoomId) {
        Assertions.assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.jsonPath().getLong("chatRoomId")).isEqualTo(expectedChatRoomId)
        );
    }

    public static void 채팅방_목록_조회_응답을_검증한다(ExtractableResponse<Response> response, int expectedSize,
                                          String expectedFirstName) {
        List<Map<String, Object>> chatRooms = response.jsonPath().getList("$");
        Assertions.assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(chatRooms).hasSize(expectedSize),
                () -> assertThat(chatRooms.get(0).get("nickname").toString()).isEqualTo(expectedFirstName),
                () -> assertThat(chatRooms.get(0).get("chatRoomId")).isNotNull(),
                () -> assertThat(chatRooms.get(0)).containsKey("lastContent")
        );
    }

    public static void 메시지_전송_응답을_검증한다(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 메시지_목록_조회_응답을_검증한다(ExtractableResponse<Response> response, int expectedSize,
                                          String firstMessageContent) {
        List<Map<String, Object>> messages = response.jsonPath().getList("$");

        Assertions.assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(messages).hasSize(expectedSize),
                () -> assertThat(messages.get(0).get("content")).isEqualTo(firstMessageContent),
                () -> assertThat(messages.get(0).get("messageId")).isNotNull()
        );
    }

    public static void 채팅방_참가자_정보를_검증한다(ExtractableResponse<Response> response, Long senderId) {
        Long parsedSenderId = response.jsonPath().getLong("senderId");

        Assertions.assertAll(
                () -> assertThat(parsedSenderId).isEqualTo(senderId)
        );
    }

    public static void 상태코드가_200이다(ExtractableResponse<Response> response) {
        Assertions.assertAll(
                () -> 상태코드를_검증한다(response, HttpStatus.OK)
        );
    }

    public static void 상태코드가_429이다(ExtractableResponse<Response> response) {
        Assertions.assertAll(
                () -> 상태코드를_검증한다(response, HttpStatus.TOO_MANY_REQUESTS)
        );
    }

    private static void 상태코드를_검증한다(ExtractableResponse<Response> response, HttpStatus expectedHttpStatus) {
        assertThat(response.statusCode()).isEqualTo(expectedHttpStatus.value());
    }

}