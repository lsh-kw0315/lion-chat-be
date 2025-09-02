package com.lion.be.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    TOKEN_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "토큰의 권한이 없습니다."),

	USER_PROFILE_INCOMPLETE(HttpStatus.BAD_GATEWAY,"기본 사용자 정보가 누락되었습니다." ),
	USER_ONBOARDING_PROFILE_INCOMPLETE(HttpStatus.BAD_GATEWAY,"온보딩 정보가 누락되어있습니다."),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "유저의 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,"이미 존재하는 닉네임입니다." ),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
    COMMENT_NOT_FOUNT(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    USER_ONBOARDING_NOT_COMPLETED(HttpStatus.UNAUTHORIZED, "온보딩을 완료해야 해당 기능을 사용할 수 있습니다."),
	USER_ONBOARDING_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST,"이미 온보딩이 완료된 사용자입니다."),

    UNIVERSITY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 대학교입니다."),

    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다."),
    CLUSTERING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"클러스터링 수행 중 오류 발생"),
	MINIMUM_PHOTOS_REQUIRED(HttpStatus.BAD_REQUEST, "최소 1장의 사진이 필요합니다."),
	MAXIMUM_PHOTOS_REQUIRED(HttpStatus.BAD_REQUEST, "최대 3장의 사진입니다."),
	INVALID_CLUSTER_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 클러스터 ID입니다."),
	INVALID_PREFERENCE_TYPE(HttpStatus.BAD_REQUEST,"지원하지 않은 타입입니다." ),

    CHAT_ROOM_CREATION_FAILED(HttpStatus.BAD_REQUEST, "채팅방 생성에 실패했습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    MESSAGE_PUBLISH_FAILED(HttpStatus.BAD_REQUEST, "메시지 전송에 실패했습니다."),

    USER_CAN_NOT_LIKE_HIMSELF(HttpStatus.BAD_REQUEST, "자기 자신에게 좋아요를 누를수 없습니다."),

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 중 에러가 발생했습니다."),

    IMAGE_DELETE_ACCESS_DENIED(HttpStatus.FORBIDDEN,"이미지 소유권자 가 아닙니다." ),
    BANNED_USER_LOGIN(HttpStatus.UNAUTHORIZED,"밴된 유저입니다. "),
    TOO_MANY_REQUESTS_PER_SECONDS_EXCEPTION(HttpStatus.TOO_MANY_REQUESTS, "초당 API 요청 횟수를 초과했습니다."),
    TOO_MANY_REQUESTS_PER_MINUTE_EXCEPTION(HttpStatus.TOO_MANY_REQUESTS, "분당 API 요청 횟수를 초과했습니다."),

    // ENUM MAPPING
    GENDER_VALUE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 성별 값입니다."),
    POSITION_VALUE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 포지션 값입니다."),
    UNIVERSITY_VALUE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 학교 값입니다."),
    PREFERENCE_TYPE_VALUE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 선호 타입 값입니다."),
    MBTI_VALUE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 MBTI 값입니다.");

    private final HttpStatus status;
    private final String message;

}
